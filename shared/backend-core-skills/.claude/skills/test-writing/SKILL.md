---
name: test-writing
description: Kotlin Spring 백엔드 테스트 작성 기준을 정리한 스킬. Controller, Service, Repository, 통합 테스트의 작성 규칙을 포함한다.
---

# Test Writing

## Purpose

이 스킬은 현재 프로젝트의 테스트 컨벤션에 맞춰 테스트 코드를 작성하거나 보강할 때 사용한다.
테스트 개수보다 회귀 방지, 계약 검증, 분기 커버 확보를 우선한다.

## 1) 공통 원칙

- 테스트 클래스는 성공/실패 또는 기능 단위로 `@Nested` 그룹을 나눈다.
- 각 테스트는 Given-When-Then 흐름을 유지하고, 테스트 이름은 한국어 시나리오 문장으로 작성한다.
- DTO 클래스명은 `DTO` suffix 기준으로 읽고 검증한다.
- 새 기능이나 분기 추가 시 Controller, Service, Repository, 통합 테스트 영향 범위를 함께 본다.
- 인증이 필요한 API는 인증 성공 시나리오를 반드시 포함하고 필요 시 비인증/권한 실패를 추가한다.
- 예외는 `CustomException`과 도메인 `*ErrorCode` 기준으로 검증한다.
- 실제 운영 응답 포맷인 `ApiResponse` 기준으로 검증한다.
- Mocking은 필요한 협력 객체에만 최소한으로 사용한다.
- 문서 도구에 테스트를 과도하게 결합하지 않고, 실제 계약과 회귀 위험을 기준으로 테스트 범위를 잡는다.

## 2) 파일 구조와 클래스 배치

- Controller 테스트: `src/test/kotlin/com/project/domain/<domain>/controller/...`
- Service 테스트: `src/test/kotlin/com/project/domain/<domain>/service/...`
- Repository 테스트: `src/test/kotlin/com/project/domain/<domain>/repository/...`
- 통합 테스트 공통 지원 코드는 기존 테스트 인프라 위치를 따른다.
- 테스트는 프로덕션 패키지 구조와 최대한 대응되게 둔다.

## 3) 테스트 범위 우선순위

### 레이어별 최소 검증 범위

- Service: 핵심 비즈니스 메서드의 성공/실패 분기를 모두 테스트한다.
- Controller: 주요 엔드포인트의 성공과 대표 실패 케이스를 함께 테스트한다.
- Repository: 커스텀 쿼리 메서드는 최소 1개 이상의 실제 조건 검증을 둔다.
- Integration: 도메인별 핵심 사용자 흐름 또는 트랜잭션 경계 시나리오를 최소 1개 이상 둔다.

### 분기 검증 우선순위

1. 권한 분기 (`NOT_AUTHORIZED`)
2. 존재 검증 분기 (`NOT_FOUND`, `NOT_EXIST`)
3. 중복/상태 충돌 분기 (`CONFLICT`)
4. 요청 검증 실패 (`INVALID_REQUEST`)
5. 동시성, 락, 캐시, 이벤트 경계

### 테스트가 약할 때 보강 방법

- 단순 getter, DTO, data class보다 서비스 분기 로직 테스트를 늘린다.
- private 로직은 public 메서드 경유로 검증 가능하게 설계한다.
- 누락된 예외 분기는 별도 실패 `@Nested` 그룹으로 보강한다.
- 회귀 가능성이 큰 버그는 통합 테스트 1건으로 묶어 재현 가능하게 남긴다.

## 4) Controller 테스트

### 기본 골격

- `@WebMvcTest(controllers = [<DomainController>::class])`를 우선 사용한다.
- `@Import(TestSecurityConfig::class)`로 JWT 필터 없이 컨트롤러 로직에만 집중한다.
- 인증이 필요한 API는 `@WithCustomUser`로 인증 사용자를 주입한다.
  - 기본값: `userId = 1L`, `email = "user@test.com"`, `role = "USER"`
  - 다른 유저 필요 시: `@WithCustomUser(userId = 2L, role = "ADMIN")`
  - 위치: `src/test/kotlin/com/project/global/security/WithCustomUser.kt`
- 요청/응답 검증은 `MockMvc` 기준으로 작성한다.
- Swagger 문서는 별도 어노테이션/설명 품질로 관리하고, 테스트는 API 계약 검증에 집중한다.
- Kotlin 테스트는 백틱 함수명, `inner class` + `@Nested`, 생성자/프로퍼티 기반 테스트 구성을 우선한다.

### 반드시 볼 검증 포인트

- HTTP status
- `$.isSuccess`
- `$.code`
- `$.result`
- 검증 실패 시 field error 또는 공통 에러 응답 구조
- 인증 실패, 권한 실패, 도메인 예외의 `ErrorCode`

### 작성 기준

- Controller 테스트는 API 경계 검증에 집중하고 서비스 내부 규칙 재현은 최소화한다.
- 인증 필요한 API는 인증 성공 시나리오 없이 비인증 실패만 두고 끝내지 않는다.
- 성공 응답은 생성 `SUCCESS-201`, 조회/수정/삭제 `SUCCESS-200` 등 실제 코드값까지 확인한다.

## 5) Service 테스트

### 기본 골격

- `@ExtendWith(MockitoExtension::class)` 기반으로 작성한다.
- `@Mock`, `@InjectMocks`를 사용해 서비스 책임만 검증한다.
- `src/test/kotlin/.../global/fixture`의 공용 fixture를 우선 사용한다.
- fixture로 표현이 어려운 조합일 때만 private helper를 최소 범위로 둔다.
- nullable 처리와 예외 검증은 Kotlin 문법에 맞게 간결하게 작성한다.

### 반드시 볼 검증 포인트

- 반환값
- 상태 변경
- 협력 객체 호출 여부와 호출 횟수
- 실패 시 `CustomException`
- `ex.errorCode` 또는 `ex.getErrorCode()`가 기대한 도메인 `*ErrorCode`와 일치하는지

### 작성 기준

- DTO 변환, 엔티티 조립, 권한 검사, 상태 전이 같은 비즈니스 규칙을 우선 검증한다.
- 단순 구현 세부보다 외부에서 관찰 가능한 결과를 본다.
- 실패 케이스는 존재 오류, 권한 오류, 중복/상태 충돌을 우선 채운다.

## 6) Repository 테스트

- `@DataJpaTest` 기반으로 실제 DB 쿼리 결과를 검증한다.
- 단순 CRUD보다 커스텀 쿼리 조건, 정렬, 커서, fetch join, 락 쿼리를 우선 테스트한다.
- 조회 조건이 많을수록 fixture 데이터를 의도적으로 분리해 false positive를 줄인다.
- 락이나 카운트 기준 쿼리는 동시성 의도를 드러내는 이름과 데이터 배치를 사용한다.

## 7) 통합 테스트

- 필요 시 `@SpringBootTest`와 `@AutoConfigureMockMvc`를 사용한다.
- Controller, Service, DB가 함께 연결된 실제 흐름을 end-to-end로 검증한다.
- 캐시, 이벤트, 트랜잭션 경계, 권한 흐름, 회귀 버그 재현 시나리오를 우선 대상으로 삼는다.
- 단위 테스트로 이미 충분한 분기는 반복하지 말고 계층 간 연결 리스크를 검증한다.

## 8) 금지/주의 사항

- 인증이 필요한 API에서 비인증 시나리오만 테스트하고 끝내지 않는다.
- `ErrorCode` 검증 없이 status만 확인하는 테스트를 남발하지 않는다.
- 실제 운영 응답 구조와 다른 임시 JSON 포맷 검증을 추가하지 않는다.
- 서비스 테스트에서 불필요하게 스프링 컨텍스트를 띄우지 않는다.
- Repository 테스트를 단순 save/findById 확인 수준으로만 채우지 않는다.

## Workflow

1. 이번 변경으로 보호해야 할 분기와 계약을 먼저 적는다.
2. Controller, Service, Repository, Integration 중 영향 레이어를 고른다.
3. 성공/실패를 `@Nested`로 나누고 한국어 시나리오 이름을 작성한다.
4. `ApiResponse`, `CustomException`, `*ErrorCode` 기준 assertion을 넣는다.
5. 실제 회귀 위험이 큰 분기부터 우선 채운다.

## Completion Gate

- 성공/실패 시나리오가 `@Nested`로 명확히 나뉘는가?
- 테스트 이름만 보고 시나리오가 이해되는가?
- `ApiResponse`와 `ErrorCode`까지 검증하는가?
- 인증 API에 인증 성공 케이스가 포함되는가?
- 핵심 비즈니스 분기와 대표 실패 케이스를 덮는가?

## Assets

- `assets/test-structure-template.md`: 테스트 클래스 구조 템플릿
