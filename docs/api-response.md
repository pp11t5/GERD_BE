# API 응답 규칙

## 1. 문서 목적
이 문서는 팀 공통 API 응답 포맷과 예외 모델을 정리한다.  
상세 설계 기준 → [api-design skill](../.claude/skills/api-design/SKILL.md)

## 2. 공통 응답 포맷

모든 API 응답은 `ApiResponse<T>` 래퍼를 사용한다.

- 단순 `200 OK` 응답은 `ApiResponse<T>`를 직접 반환할 수 있다
- 성공 HTTP status를 `200` 외로 제어해야 하면 `ResponseEntity<ApiResponse<T>>`를 사용한다
- `ResponseEntity<ApiResponse<?>>` 같은 와일드카드 타입보다 `ResponseEntity<ApiResponse<AuthTokenResponseDTO>>`처럼 구체 result 타입을 드러내는 방식을 기본값으로 사용한다
- 성공 응답의 body `code/message`와 실제 HTTP status는 같은 `BaseSuccessCode` 구현체에서 나오도록 맞춘다
- 수정/삭제처럼 별도 반환 데이터가 없는 성공 응답은 `ResponseEntity<ApiResponse<Unit?>>`를 사용하고 `result`는 `null`로 내려준다
- `result` 필드는 항상 응답 body에 포함한다. 데이터가 없다는 의미는 필드 생략이 아니라 `null`로 표현한다

### 성공 응답 예시 - result 있음

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공하였습니다.",
  "result": { }
}
```

### 성공 응답 예시 - result 없음

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공하였습니다.",
  "result": null
}
```

### 실패 응답 예시

```json
{
  "isSuccess": false,
  "code": "MEMBER4001",
  "message": "회원을 찾을 수 없습니다.",
  "result": null
}
```

### 성공 응답 작성 예시

```kotlin
override fun refresh(
    @Valid @RequestBody request: RefreshTokenRequestDTO,
): ResponseEntity<ApiResponse<AuthTokenResponseDTO>> =
    ResponseEntity
        .status(CommonSuccessCode.OK.httpStatus)
        .body(ApiResponse.onSuccess(authService.refresh(request.refreshToken), CommonSuccessCode.OK))
```

반환 데이터가 없는 성공 응답은 전용 `onSuccess()` 헬퍼를 사용한다.

```kotlin
override fun delete(
    @CurrentUser userDetails: CustomUserDetails,
    @PathVariable symptomId: String,
): ResponseEntity<ApiResponse<Unit?>> {
    symptomService.delete(symptomId, userDetails.userId)
    return ResponseEntity
        .status(CommonSuccessCode.OK.httpStatus)
        .body(ApiResponse.onSuccess())
}
```

## 3. HTTP 상태코드 기준

| 상황 | 상태코드 |
|---|---|
| 성공 (조회/생성/수정) | 200 / 201 |
| 유효성 검증 실패 | 400 |
| 인증 실패 | 401 |
| 권한 없음 | 403 |
| 리소스 없음 | 404 |
| 비즈니스 충돌 | 409 |
| 서버 오류 | 500 |

- 생성 API는 `201 Created`를 우선 검토한다
- 비동기 접수는 `202 Accepted`를 검토한다
- 현재 프로젝트는 모든 API를 `ApiResponse` body로 감싸므로 `204 No Content`는 기본 선택지로 두지 않는다. 반환 데이터가 없는 성공 응답도 `200 OK`와 `result: null`을 사용한다

## 4. 에러 코드 규칙

- 형식: `{도메인}{HTTP상태코드}{순번}` (예: `MEMBER4001`)
- 에러 코드는 도메인별 `*ErrorCode` enum으로 관리
- 클라이언트에 내부 구현 정보 노출 금지

## 5. 성공 코드 규칙

- 성공 코드는 `BaseSuccessCode` 인터페이스로 추상화한다
- 공통 성공 코드는 `CommonSuccessCode` enum으로 관리한다
- 도메인 고유 성공 응답이 필요하면 각 도메인 패키지에 `*SuccessCode` enum을 추가하고 `BaseSuccessCode`를 구현한다
- `code`, `message`, `httpStatus`는 한 성공 코드 정의에서 함께 관리한다

## 6. DTO 규칙

- 요청/응답 DTO는 Entity와 분리
- 응답 DTO에 Entity 전체 반환 금지
- DTO 클래스명은 `DTO`로 끝냄
- `data class` 우선, setter/builder 필요 시 `class`
- DTO에 비즈니스 로직 금지

## 7. 검증 규칙

- `@RequestBody` DTO 검증은 Controller 메서드 파라미터에 `@Valid`를 붙인다
- `@PathVariable`, `@RequestParam`, `@ModelAttribute`에 선언한 제약 검증을 활성화하려면 Controller 클래스 또는 인터페이스에 `@Validated`를 붙인다
- `@Validated`가 적용된 파라미터 검증 실패는 주로 `ConstraintViolationException`으로 전파되므로 전역 예외 처리와 함께 본다
- UUID 문자열은 정규식 대신 Hibernate Validator의 `@field:UUID(message = "...")`를 사용한다
  - 예: `@field:UUID(message = "끼니 식별자는 UUID 형식이어야 합니다.")`
- ISO-8601 offset 시각 문자열은 정규식 대신 공통 커스텀 검증 어노테이션 `@field:ValidOffsetDateTime(message = "...")`를 사용한다
  - 예: `@field:ValidOffsetDateTime(message = "증상 발생 시각은 ISO-8601 offset 형식이어야 합니다.")`
  - 증상 DTO와 식사 DTO 모두 동일한 기준을 적용한다

## 8. Swagger 규칙

- 공통 응답 래퍼는 `SwaggerConfig`에서 일괄 처리
- 엔드포인트에는 고유 result 의미와 실패 조건만 기술
- `@Operation`, `@ApiResponses` 한국어로 작성
- 성공 응답 `responseCode`는 실제 `BaseSuccessCode.httpStatus`와 일치해야 한다

## 9. 인증 사용자 파라미터

- 인증 사용자 주입은 팀 공통 규칙(`@CurrentUser` 또는 커스텀 파라미터 리졸버) 사용
- Controller에서 직접 SecurityContext를 건드리지 않음
