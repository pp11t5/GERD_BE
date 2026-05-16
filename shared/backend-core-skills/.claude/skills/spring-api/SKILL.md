---
name: spring-api
description: Spring Boot 기반 Controller/API 구현 규칙을 정리한 스킬. 인증 사용자 파라미터(@CurrentUser), [Domain]Api 인터페이스 분리, 예외 매핑, 응답 일관성에 집중한다.
---

# Spring API

## Purpose

이 스킬은 설계된 API 계약을 Kotlin + Spring Boot API 계층에 안정적으로 구현할 때 사용한다.
Controller는 얇게 유지하고, 요청 검증과 응답 변환에만 집중한다.

## Critical Rules

- Controller는 API 입출력과 권한 확인만 처리한다.
- Controller에 비즈니스 로직을 넣지 않는다.
- Request DTO에서 1차 검증을 수행한다.
- DTO 클래스명은 `DTO`로 끝낸다.
- 인증 사용자는 `@CurrentUser User user` 파라미터로 주입받는다. `@AuthenticationPrincipal`을 Controller에 직접 노출하지 않는다.
- 예외는 Controller에서 삼키지 않고 전역 핸들러로 위임한다.
- 응답은 공통 `ApiResponse` 포맷을 유지한다.
- 서비스는 비즈니스 로직만 담당하고 `ApiResponse` 생성은 Controller/API 계층에서 처리한다.
- Swagger 설명은 `[Domain]Api` 인터페이스에만 작성하고, Controller 구현체에는 작성하지 않는다.
- 도메인 예외 문서는 `@ApiErrorExample(...)`로 선언하고, 공통 응답(`400`, `500`)은 SwaggerConfig 자동 주입을 신뢰한다.
- DTO는 스키마 표현에 집중하고, 반복되거나 조합이 있는 변환은 converter로 분리한다.
- 메서드 분리는 읽기 쉬움과 변경 단위 분리가 목적일 때만 수행한다.
- 한 번만 쓰이는 1~2줄 래퍼 private 메서드를 습관적으로 만들지 않는다.

## [Domain]Api 인터페이스 분리 규칙

Controller는 구현 책임만 가지고, Swagger 설명은 `[Domain]Api` 인터페이스에 분리한다.

### 파일 구조

```
controller/
├── [Domain]Api.kt          # Swagger 어노테이션 + 메서드 시그니처
└── [Domain]Controller.kt   # @RestController, 구현체
```

### [Domain]Api 인터페이스 작성 기준

- `@Tag`, `@Operation`, `@ApiResponses`는 인터페이스에만 선언한다.
- 도메인 실패 케이스는 인터페이스 메서드에 `@ApiErrorExample(...)`로 선언한다.
- 메서드 시그니처에 `@PathVariable`, `@RequestParam`, `@RequestBody`, `@CurrentUser` 선언한다.
- `@Valid`는 Controller 구현체의 `override` 메서드에 선언한다.
- 인터페이스에는 기본 구현을 두지 않는다.

### [Domain]Controller 작성 기준

- `[Domain]Api`를 구현(`implements`)한다.
- `@RestController`, `@RequestMapping`을 선언한다.
- `@Operation`, `@ApiResponses`는 작성하지 않는다.
- `override` 메서드에서 `@Valid`를 붙여 검증한다.
- Service에 위임하고 `ApiResponse.onSuccess(...)` 반환만 수행한다.

### 예시

```kotlin
// [Domain]Api.kt
@Tag(name = "[Domain]", description = "[도메인] API")
interface [Domain]Api {

    @Operation(summary = "[도메인] 목록 조회", description = "...")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "조회 성공")])
    @ApiErrorExample([Domain]ErrorCode.NOT_FOUND, [Domain]ErrorCode.FORBIDDEN)
    @GetMapping("/[domains]")
    fun get[Domains](
        @CurrentUser user: User,
        @PathVariable [domain]Id: Long,
    ): ApiResponse<[Domain]ResponseDTO.[Domain]SliceDTO>
}

// [Domain]Controller.kt
@RestController
@RequestMapping("/api/v1")
class [Domain]Controller(
    private val [domain]QueryService: [Domain]QueryService,
) : [Domain]Api {

    override fun get[Domains](
        @CurrentUser user: User,
        @PathVariable [domain]Id: Long,
    ): ApiResponse<[Domain]ResponseDTO.[Domain]SliceDTO> =
        ApiResponse.onSuccess([domain]QueryService.get[Domains](user.id, [domain]Id))
}
```

## @CurrentUser 사용 규칙

- 인증 사용자 주입은 `@CurrentUser User user` 파라미터를 사용한다.
- `@AuthenticationPrincipal`을 Controller에 직접 노출하지 않는다.
- `CustomUserDetails` 타입을 Controller 파라미터로 노출하지 않는다.
- `@CurrentUser`는 커스텀 어노테이션 + `HandlerMethodArgumentResolver`로 구현한다.
- Resolver 내부에서 `CustomUserDetails` → `User` 변환을 처리한다.

구현 예시 → `assets/current-user-annotation-example.kt`

## Swagger Rules

- `@Operation`과 `@ApiResponses`는 `[Domain]Api` 인터페이스에 한국어로 작성한다.
- 공통 응답 래퍼와 공통 에러 응답은 SwaggerConfig에서 처리한다고 가정하고, 엔드포인트 고유 의미만 설명한다.
- `400`, `500`은 SwaggerConfig가 모든 operation에 자동 추가한다. 인터페이스에서 중복 선언하지 않는다.
- `401`은 `@CurrentUser`가 있는 operation에만 SwaggerConfig가 자동 추가한다. 인증 API라고 해서 수동으로 매번 `401`을 적지 않는다.
- `@ApiErrorExample(...)`에 선언한 도메인 에러는 상태코드별 예시(example)로 문서화된다.
- 인증이 필요 없는 공개 API에는 `@CurrentUser`를 넣지 않는다. 그러면 Swagger에도 보안 요구사항과 `401`이 붙지 않는다.
- 설명은 요청값, 성공 조건, 주요 실패 조건 순으로 작성한다.
- `responseCode`는 실제 `SuccessStatus`의 HTTP status와 맞춘다.
- `Void` 응답이더라도 사용자에게 의미 있는 성공 결과가 있으면 `ApiResponse<String>` 또는 DTO 반환을 먼저 검토한다.

### SwaggerConfig 연동 규칙

- `SwaggerConfig`는 전역으로 JWT 보안을 걸지 않는다.
- `@CurrentUser`가 있는 operation만 JWT security requirement와 `401` 응답이 자동 추가된다.
- 도메인 예외 예시는 `@ApiErrorExample(...)`에 넣은 에러코드 enum을 읽어서 생성한다.
- 따라서 인터페이스에서는 `200/201` 같은 성공 응답만 명시하고, 도메인 실패는 `@ApiErrorExample(...)`로 표현하는 패턴을 기본값으로 사용한다.
- 현재 구현 기준 `@ApiErrorExample` 파라미터 타입은 도메인별 enum이다. 예: `@ApiErrorExample(AuthErrorCode.USER_NOT_FOUND)`.

### Swagger 예시 작성 패턴

```kotlin
@Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스/리프레시 토큰을 재발급")
@ApiErrorExample(
    AuthErrorCode.INVALID_TOKEN,
    AuthErrorCode.EXPIRED_TOKEN,
    AuthErrorCode.USER_NOT_FOUND,
)
@ApiResponses(
    ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
)
@PostMapping("/refresh")
fun refresh(
    @RequestBody request: RefreshTokenRequestDTO,
): ApiResponse<AuthTokenResponseDTO>
```

위 패턴이면 Swagger에는 다음이 반영된다.

- `200`: 인터페이스에 명시한 성공 응답
- `400`, `500`: SwaggerConfig 자동 추가
- `404`: `USER_NOT_FOUND` 예시 자동 추가
- `401`: `INVALID_TOKEN`, `EXPIRED_TOKEN` 예시 자동 추가

주의:
- `@CurrentUser`가 없는 공개 API에는 `401`이 자동 추가되지 않는다.
- `@ApiResponses`에 `401`, `404`를 문자열로 반복해서 적기보다 `@ApiErrorExample(...)`를 우선 사용한다.

## Request DTO Validation

이 프로젝트는 `-Xannotation-default-target=param-property` 컴파일러 옵션이 설정되어 있어,  
생성자 파라미터에 선언한 어노테이션이 자동으로 필드에 전파된다. `@field:` 명시는 불필요하다.

### 어노테이션 선언 기준

| 어노테이션 | 용도 | message 필수 여부 |
|---|---|---|
| `@NotBlank` | 빈 문자열 · 공백 방지 | 필수 |
| `@Size` | 길이 범위 제한 | 필수 |
| `@Pattern` | 정규식 형식 검증 | 필수 |
| `@NotNull` | null 방지 (원시 타입 대체) | 필수 |

- **모든 어노테이션에 `message`를 명시한다.** 글로벌 핸들러가 `message` 값을 그대로 클라이언트에 반환하므로, 생략하면 기본 영문 메시지가 노출된다.
- `message`는 사용자에게 노출되는 문장으로 작성한다 (예: "제목은 필수입니다").
- nullable 필드에 검증이 필요한 경우 `@NotBlank val title: String?` 형태로 선언한다. 값 부재 시 `MethodArgumentNotValidException`으로 처리된다.

### 예시

```kotlin
data class CreatePostRequestDTO(
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요")
    val title: String,

    @Size(max = 1000, message = "본문은 1000자 이하로 입력해 주세요")
    val content: String?,

    @NotBlank(message = "슬러그는 필수입니다")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "슬러그는 영소문자, 숫자, 하이픈만 허용됩니다")
    val slug: String,

    @NotNull(message = "카테고리 ID는 필수입니다")
    val categoryId: Long,
)
```

### 검증 오류 응답 흐름

1. `@Valid` → `MethodArgumentNotValidException` 발생
2. 전역 `@ExceptionHandler`에서 `BindingResult`의 `message`를 수집
3. `ApiResponse` 에러 포맷으로 클라이언트에 반환

## Workflow

1. API 설계 문서를 기준으로 method/path를 확정한다.
2. `[Domain]Api` 인터페이스를 작성하고 Swagger 설명을 붙인다.
3. Request DTO와 검증 어노테이션을 작성한다.
4. `[Domain]Controller`에서 인터페이스를 구현하고 Service에 위임한다.
5. 응답 DTO 또는 공통 Response로 변환한다.
6. 인증/인가, 예외 매핑, 상태코드를 검증한다.

## Implementation Rules

- 메서드명은 `create`, `get`, `search`, `update`, `delete`처럼 의도를 드러낸다.
- Query 파라미터는 검색/정렬/페이지네이션 의미를 이름으로 분리한다.
- 파일 업로드, 멀티파트, 비동기 응답은 일반 JSON API와 별도 규칙으로 다룬다.
- `ResponseEntity`가 필요 없으면 프레임워크 표준 응답을 간결하게 유지한다.
- 주석은 무엇보다 왜를 설명하고, 한 줄 주석을 기본으로 사용한다.
- 주석 문장 끝에는 마침표를 붙이지 않고, 기존 코드의 톤과 길이를 맞춘다.

## Completion Gate

- `[Domain]Api` 인터페이스와 `[Domain]Controller`가 분리됐는가?
- Controller에 `@Operation`, `@ApiResponses`가 남아 있지 않은가?
- 도메인 에러가 `@ApiErrorExample(...)`로 선언됐는가?
- 인증 사용자가 `@CurrentUser User user`로 주입되는가?
- Controller가 Service 호출 외의 핵심 로직을 가지지 않는가?
- Validation이 DTO와 API 계약에 맞게 선언됐는가?
- 전역 에러 핸들링과 충돌하지 않는가?

## Assets

- `assets/controller-dto-template.kt`: RequestDto(검증 포함) · ResponseDto · Api 인터페이스 · Controller 전체 템플릿
- `assets/swagger-operation-example.kt`: `[Domain]Api` 인터페이스 + Controller 분리 예시
- `assets/current-user-annotation-example.kt`: `@CurrentUser` 어노테이션, Resolver, WebMvcConfig 구현 예시
