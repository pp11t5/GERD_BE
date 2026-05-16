---
name: api-design
description: 백엔드 팀의 API 설계 기준을 고정하는 스킬. 응답 규칙, 예외 모델, 버전 호환성, 템플릿 기반 초안 작성을 포함한다.
---

# API Design

## Purpose

이 스킬은 새로운 API를 설계하거나 기존 계약을 수정할 때 사용한다.
엔드포인트 형태보다 계약 일관성, 확장성, 팀 공통 응답 규칙을 우선한다.

## Critical Rules

- API는 리소스/행위 의도가 경로와 메서드에서 설명 가능해야 한다.
- 요청/응답 DTO는 Entity와 분리한다.
- DTO는 요청/응답 스키마 역할에만 집중한다.
- DTO 내부에 엔티티 생성/수정 로직을 넣지 않는다.
- 성공/실패 응답 구조는 공통 `ApiResponse` 규칙을 따른다.
- Validation 실패, 인증 실패, 권한 실패, 도메인 예외를 처음부터 설계 범위에 포함한다.
- 계약 변경은 하위 호환 여부를 먼저 판단하고, 파괴적 변경이면 명시적으로 기록한다.
- 초안 작성은 가능한 한 템플릿을 먼저 사용하고 그 위에 도메인 규칙을 얹는다.
- 공통 Swagger 응답 설명은 SwaggerConfig가 담당한다고 가정하고, 엔드포인트 설계에서는 고유한 result 의미와 실패 조건만 남긴다.
- 예외는 `CustomException` + 도메인 `*ErrorCode` 기준으로 맞추고 내부 메시지/스택은 외부에 노출하지 않는다.
- 템플릿은 시작점일 뿐이며 원문을 그대로 커밋하지 않는다.
- DTO는 스키마 역할에 집중하고 엔티티 생성 로직을 넣지 않는다.

## Primary Template Usage

이 스킬은 `templates`를 가장 우선적으로 활용한다.
초안은 아래 템플릿을 시작점으로 잡고, 도메인 규칙에 맞게 보정한다.

- `controller-api.template.kt`: 기본 CRUD/단건 유즈케이스 API 초안
- `controller-api-cqrs.template.kt`: 조회/명령 분리가 필요한 API 초안
- `request-dto.template.kt`: 요청 DTO 초안
- `response-dto.template.kt`: 응답 DTO 초안
- `api-response-format.template.json`: 공통 `ApiResponse` 포맷 초안
- `error-code-enum.template.kt`: 에러 코드 초안
- `current-user-controller-param.template.kt`: 인증 사용자 파라미터 초안

## DTO Conventions

- DTO 클래스명은 `DTO`로 끝낸다
- 요청 DTO는 `CreatePostRequestDTO`, 응답 DTO는 `CreatePostResponseDTO`처럼 역할 + `DTO` suffix를 사용한다
- repository projection DTO도 `DTO`로 끝내고 필요하면 `Projection`, `Summary`, `Detail` 같은 역할명을 앞에 둔다

### DTO 타입 선택 기준

- `data class`를 우선 쓰는 경우
  - 단순 불변 요청 DTO
  - 단순 불변 응답 DTO
  - 필드가 명확하고 setter가 필요 없는 경우
  - 생성 이후 값을 바꾸지 않아야 하는 계약 DTO
  - equals/hashCode/toString의 값 객체 성격이 잘 맞는 경우

- class를 쓰는 경우
  - 계층형 응답 묶음이 필요할 때
  - setter 또는 후처리가 필요한 응답일 때
  - builder 가독성이 더 좋은 경우
  - `@JsonInclude`, builder, 기본 생성자 등 클래스 기반 어노테이션 조합이 필요한 경우
  - 현재 코드와의 일관성을 위해 기존 class 스타일을 유지하는 편이 더 자연스러운 경우

### `data class` vs class 빠른 판단표

- 아래에 모두 해당하면 `data class`를 기본으로 쓴다
  - 불변이어야 한다
  - 필드가 단순하다
  - 생성 후 후처리가 없다
  - builder가 없어도 읽기 쉽다

- 아래 중 하나라도 있으면 class를 검토한다
  - setter가 필요하다
  - builder가 더 읽기 쉽다
  - 클래스 기반 어노테이션 조합이 필요하다
  - 기존 같은 파일의 DTO들이 class 스타일로 묶여 있다
  - 프레임워크 바인딩이나 점진적 마이그레이션 때문에 mutable 구조가 더 안전하다

### 현재 프로젝트 기준 권장 선택

- 새 요청 DTO
  - 기본은 `data class`
  - 예외적으로 setter/기본 생성자가 꼭 필요할 때만 class

- 새 응답 DTO
  - 기본은 `data class`
  - 응답 후처리나 builder 조합이 필요하면 class

- 여러 응답 타입을 한 파일에 묶는 경우
  - 단순 응답은 nested `data class`
  - mutable/후처리 응답만 nested class

### 현재 프로젝트에서 읽히는 예외 케이스

- 기본은 `data class`
- 후처리, builder, mutable binding 필요 시 class
- 즉 무조건 `data class`가 아니라 혼합 전략을 사용한다

### 요청 DTO 규칙

- 요청 DTO는 `data class`를 기본으로 사용한다.
- Bean Validation은 DTO 필드에 직접 선언한다.
- 컨트롤러 검증에 필요한 형식 제약만 둔다.
- 비즈니스 규칙 검증은 서비스에서 수행한다.
- 새 코드에서는 특별한 이유가 없으면 `data class`를 우선한다.
- 기존 mutable request DTO를 수정할 때는 주변 코드 스타일과 바인딩 방식을 먼저 확인한다.

### Bean Validation 어노테이션 선언 기준

현재 프로젝트는 `-Xannotation-default-target=param-property` 컴파일러 옵션이 설정되어 있다.  
이 옵션으로 생성자 파라미터에 선언한 어노테이션이 **자동으로 필드에 전달**된다.

```kotlin
// 이 프로젝트에서는 @NotBlank만 써도 동작한다
data class CreatePostRequest(
    @NotBlank val title: String,
    @Size(max = 1000) val content: String,
    @NotNull val categoryId: Long,
)

// @field: 명시는 불필요하지만 팀 내 명시적 표현을 선호하면 허용한다
data class CreatePostRequest(
    @field:NotBlank val title: String,
)
```

- nullable 필드 + validation: `@NotBlank val title: String?` 형태로 nullable + 검증을 함께 쓸 수 있다. 값이 없으면 `MethodArgumentNotValidException`으로 처리된다.
- `@field:` 접두사는 컴파일러 옵션이 없는 프로젝트에서 필수. 현재 프로젝트는 불필요하지만 명시해도 무방하다.

### 응답 DTO 규칙

- 응답 DTO는 외부로 노출할 값만 담는다.
- 엔티티 전체를 그대로 반환하지 않는다.
- 표시용 문자열, 요약값, 경량 필드를 별도로 만들 수 있다.
- 후처리 가능한 mutable DTO보다 최종 값 계산 후 DTO 생성 방식을 우선한다.
 - 후처리 없는 응답은 `data class`를 우선한다.
 - 후처리 setter, builder, nullable field 제어가 필요하면 class를 허용한다.

### DTO 생성 위치 기준

- converter에서 생성하는 경우
  - 동일한 엔티티 -> DTO 변환이 반복될 때
  - 부작용 없는 순수 매핑일 때
  - 서비스 여러 곳에서 재사용될 때

- service에서 직접 생성하는 경우
  - 단발성 응답 조립일 때
  - 여러 소스의 데이터를 합쳐야 할 때
  - 후처리나 조건 분기가 서비스 문맥에 더 가까울 때

### DTO 파일 구조 기준

- 도메인별 `dto` 패키지에 둔다.
- 관련 DTO가 적으면 `<Domain>RequestDTO`, `<Domain>ResponseDTO`로 묶는다.
- 여러 응답 조합이 있으면 상위 래퍼 클래스 안에 nested DTO를 둔다.
- 조회 전용 repository DTO는 `repository/dto` 아래에 둔다.

### repository 조회용 DTO 기준

- QueryDSL projection이나 집계 전용 DTO는 repository 전용 패키지로 분리한다.
- API 응답 DTO와 repository DTO를 무조건 동일하게 맞추지 않는다.
- repository DTO는 조회 효율과 projection 구조를 우선한다.
- API 응답 DTO는 외부 계약과 표현을 우선한다.

### setter 사용 기준

- DTO는 기본적으로 불변을 우선한다.
- 후처리 단계가 명확할 때만 setter를 허용한다.
- setter 남용보다 생성 시점에 최종 값을 계산해 DTO를 만드는 방식을 우선 고려한다.

### 금지/주의 사항

- DTO에 `toEntity()`를 넣지 않는다.
- DTO에 repository/service 의존성을 넣지 않는다.
- entity를 그대로 serialize해서 DTO를 생략하지 않는다.
- request DTO에 비즈니스 상태 변경 메서드를 넣지 않는다.
 - API 응답 DTO를 JPA projection 규격에 맞추기 위해 억지로 변형하지 않는다.
 - 단순 DTO인데 습관적으로 builder class를 만들지 않는다.
 - mutable 필요가 없는데 setter를 열어두지 않는다.

## JPQL Projection Conventions

- 단순 JPQL은 엔티티 조회 또는 scalar 조회를 우선 사용한다.
- DTO projection이 복잡해지면 JPQL보다 QueryDSL custom repository로 넘긴다.
- API 응답 DTO는 service/converter에서 조립하는 패턴을 우선한다.

### 권장 선택 순서

1. 단순 엔티티 조회면 JPQL 또는 파생 메서드
2. 단순 scalar 조회면 JPQL projection
3. 복잡 DTO projection이면 QueryDSL custom repository
4. API 응답 조립은 service/converter에서 마무리

### `select new DTO(...)`를 기본으로 두지 않는 이유

- API DTO가 JPQL 생성자 시그니처에 끌려가기 쉽다.
- 응답 DTO와 조회 DTO의 책임이 섞인다.
- 집계, 동적 정렬, 커서, 서브쿼리가 들어가면 QueryDSL 쪽이 더 읽기 쉽다.

### JPQL projection을 써도 되는 경우

- 단일 컬럼 또는 소수 컬럼만 필요할 때
- 반환 타입이 scalar일 때
- 복잡한 동적 조건이 없을 때

### 금지/주의 사항

- API 응답 DTO를 JPQL `select new` 전용 구조로 억지로 맞추지 않는다.
- 단순 entity 조회까지 DTO projection으로 과설계하지 않는다.
- projection 필드가 많아지면 custom repository 전환을 검토한다.

## Workflow

### Step 1: 계약 정의

1. 리소스와 유즈케이스를 한 문장으로 정리한다.
2. HTTP method, path, 상태코드를 확정한다.
3. 요청 필수값, optional 값, 기본값을 분리한다.
4. 응답 필드 중 클라이언트가 실제로 소비하는 값만 남긴다.

### Step 2: 템플릿으로 초안 생성

1. `templates`에서 가장 근접한 controller/DTO 템플릿을 고른다.
2. `<Domain>`, `<domain>`, `<ErrorCode>` 같은 플레이스홀더를 치환한다.
3. 공통 `ApiResponse` 규칙과 SwaggerConfig 기준에 맞게 result/error 구조를 맞춘다.
4. 인증/인가가 필요한 경우 current user 파라미터 규칙을 반영한다.
5. 템플릿에 남은 placeholder, 불필요한 field, 과한 응답 필드를 제거한다.
6. 템플릿의 중첩 DTO 구조나 응답 타입이 현재 도메인에 맞는지 다시 다듬는다.

### Step 3: 실패 계약 설계

API 설계 시 아래 5가지 실패 케이스를 미리 정의하고, 각각에 맞는 에러 코드와 HTTP 상태코드를 확정한다.

#### 1. Validation 실패

- DTO 필드 단위 검증 실패 → `400 Bad Request`
- `MethodArgumentNotValidException`을 전역 핸들러에서 수집해 `ApiResponse` 에러 포맷으로 반환한다.
- 에러 응답에는 어느 필드가 왜 실패했는지 `message`를 그대로 포함한다.
- 전용 `ErrorCode`를 별도로 만들지 않고 공통 Validation 에러 코드를 재사용한다.

#### 2. 리소스 없음

- 조회/수정/삭제 대상이 존재하지 않을 때 → `404 Not Found`
- 도메인별 `*ErrorCode`에 `{RESOURCE}_NOT_FOUND`를 정의한다.
- 내부 ID나 쿼리 조건은 응답에 노출하지 않는다. 사용자 노출 메시지만 반환한다.

#### 3. 중복/상태 충돌

- 이미 존재하는 리소스 생성 요청, 허용되지 않는 상태 전환 → `409 Conflict`
- 에러 코드에 충돌 원인을 담는다 (예: `EMAIL_ALREADY_EXISTS`, `ORDER_ALREADY_CANCELLED`).
- 클라이언트가 재시도 가능 여부를 판단할 수 있도록 메시지에 이유를 명시한다.

#### 4. 인증/인가 실패

- 토큰 없음·만료·위변조 → `401 Unauthorized`
- 리소스 접근 권한 없음 → `403 Forbidden`
- 두 케이스를 동일한 에러 코드로 묶지 않는다. 클라이언트 처리 방식이 다르다.
- 403 응답에 "어떤 권한이 필요한지"는 노출하지 않는다 (보안).

#### 5. 외부 시스템 실패

- 외부 API 호출, 메시지 브로커, 이메일 발송 등 외부 연동 실패 → `502 Bad Gateway` 또는 `503 Service Unavailable`
- 외부 시스템의 내부 에러 메시지, 스택 트레이스, 엔드포인트 정보를 응답에 포함하지 않는다.
- 사용자에게는 "일시적인 오류가 발생했습니다" 수준의 메시지만 노출한다.
- 재시도 가능성이 있는 경우 `Retry-After` 헤더 또는 메시지로 안내한다.

### Step 4: 구현 연결

1. 단순 유즈케이스면 단일 Service
2. 읽기/쓰기 관심사가 갈리면 CQRS Controller 템플릿 사용
3. 응답 필드가 많아지면 DTO 조합 또는 summary/detail 응답 분리

## API Checklist

- 경로가 리소스 중심이며 url이 docs/api-response에 따라 rest api 방식으로 설계되었는가?
- 상태코드가 의미에 맞으며 docs/api-response 규칙과 일치하는가?
- 요청/응답 DTO가 Entity에 종속되지 않는가?
- 에러 코드가 도메인 의미를 가지는가?
- .claude/skills/querydsl의 조건을 따르는 정렬/페이지네이션/필터 파라미터가 일관적인가?
- 클라이언트가 해석할 수 없는 내부 구현 정보가 응답에 섞이지 않았는가?
- 템플릿의 보일러플레이트가 도메인 규칙 없이 남아 있지 않은가?
- DTO가 스키마 역할에만 집중하는가?
- JPQL projection보다 service/converter 조립이 더 자연스러운 경우를 놓치지 않았는가?

## Output Format

```markdown
## API Summary
- Method/Path:
- 목적:

## Request
- PathVariable / Query / Body:
- Validation:

## Response
- Success status/body:
- Error cases:

## Template Base
- controller:
- request dto:
- response dto:
- error code:

## Compatibility
- 하위 호환 영향:
- 후속 작업:
```

## References

- `references/dto-checklist.md`: DTO 종류/타입 선택/생성 위치/경계 점검표
- `references/jpql-projection-checklist.md`: JPQL projection 전략 및 QueryDSL 전환 시점 점검표

## When Not To Use

- 구현만 수정하고 외부 계약은 바뀌지 않는 내부 리팩터링
