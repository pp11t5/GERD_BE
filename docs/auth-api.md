# 인증 API

## 1. 개요

카카오 OIDC 기반 소셜 로그인, JWT 발급/갱신/로그아웃, 회원탈퇴 및 계정 복구 흐름을 구현한다.

---

## 2. 엔드포인트 목록

| 메서드 | 엔드포인트 | 설명 |
|---|---|---|
| `POST` | `/api/v1/auth/{provider}/login` | OIDC ID Token 검증 후 신규 가입 또는 로그인 |
| `POST` | `/api/v1/auth/refresh` | Refresh Token 로테이션으로 토큰 재발급 |
| `DELETE` | `/api/v1/auth/logout` | DB Refresh Token 삭제 |
| `DELETE` | `/api/v1/auth/withdraw` | 상태를 `DELETED`로 변경, 전 기기 Refresh Token 즉시 만료 |
| `POST` | `/api/v1/auth/{provider}/recover` | 탈퇴 유예(14일) 중 계정 복구 |
| `POST` | `/api/v1/auth/dev-login` | 닉네임으로 토큰 발급 (dev/local 프로파일 전용) |

---

## 3. JWT / 보안 설계

### 3-1. Stateless JWT

- Access Token 만료: **1시간**
- Refresh Token 만료: **3일**
- claims에서 직접 인증 객체를 구성해 매 요청마다 DB 조회 없음

### 3-2. Refresh Token 저장소

- `refresh_tokens` 테이블에 `userId`를 PK로 저장 (단일 세션 — `save()`가 upsert로 동작)
- `sha256(tokenValue)`를 저장해 원문 유출 방지
- JTI(`uuid`) 포함, `expires_at`으로 만료 관리

### 3-3. Refresh Token 로테이션

- 재발급 시 DB에서 `sha256(refreshToken)` 조회
- 없으면 탈취 의심 → 해당 유저 전체 세션 삭제 후 차단

### 3-4. 파일 구조

- `global/security` → `domain/auth/security`로 이동해 도메인 응집도 향상

### 3-5. dev-login 운영 차단

- `@Profile("!prod")`가 붙은 `DevAuthController`로 분리
- prod 프로파일에서 빈 미등록

### 3-6. 관리자 권한 분기 (TODO)

- `WebSecurityConfig`에 `ROLE_ADMIN` 분기만 존재 (`/api/v1/admin/**`, `/v3/api-docs/admin`)
- 관리자 로그인 API 미구현 — 추후 별도 이슈로 구현 필요

---

## 4. 소셜 로그인 (OIDC)

- 카카오 OIDC: JWKS 공개키 조회 → RSA 서명 검증 → `sub` / `email` / `nickname` 추출
- `OidcVerifierRegistry`로 provider별 검증기 분기, Google 등 추가 provider 확장 가능
- 동일 이메일로 다른 provider 계정이 이미 있으면 계정 연결 처리

---

## 5. 회원탈퇴

- `status = DELETED` 전환 + 전 기기 Refresh Token 즉시 삭제 (14일 유예, 복구 가능)
- 14일 경과 후 스케줄러가 카카오 Unlink → `hardDelete` 실행 (스케줄러 구현 TODO)
- 연관 엔티티(`auth_accounts` 등)는 `@OnDelete(action = CASCADE)`로 DB 레벨 연쇄 삭제

**Soft Delete 전략**

`@SQLRestriction("deleted_at IS NULL")`을 User 엔티티에 적용한다.

| 방식 | 채택 여부 | 이유 |
|---|---|---|
| `@SQLRestriction("deleted_at IS NULL")` | **채택** | 소프트 딜리트를 `withdraw()` 도메인 메서드로 직접 처리하므로 DELETE 훅은 불필요. `@SQLRestriction` 하나로 모든 `findBy...` 쿼리에 자동 필터만 추가 |
| `@SoftDelete` | 미채택 | `repository.delete()` 호출 시 UPDATE로 교체하는 훅이 주 기능. 현재 코드는 `withdraw()` 메서드를 사용하므로 이 훅이 동작하지 않음 |
| `@SQLDelete` + `@SQLRestriction` | 미채택 | `@SQLDelete`는 `@SoftDelete`와 같은 이유로 불필요. Hibernate 6.4+ 에서는 `@SQLRestriction`만으로 충분 |

**복구 흐름에서의 필터 우회**

`@SQLRestriction`은 모든 User 쿼리에 `deleted_at IS NULL`을 추가하므로, 탈퇴 유예 유저를 조회해야 하는 recovery에서는 별도로 우회한다.

- `AuthAccount.userId`: `user_id` FK를 `insertable = false, updatable = false`로 별도 노출 — `user` lazy load 없이 ID 접근
- `UserRepository.findByIdIncludingDeleted`: 네이티브 쿼리로 `@SQLRestriction` 우회
- `OAuthService.recoverAccount`: `authAccount.userId`로 ID를 가져와 `findByIdIncludingDeleted` 호출

**Hard Delete**

JPQL `@Modifying @Query("DELETE FROM User WHERE id = :userId")`는 `@SQLRestriction` 필터를 우회해 물리 삭제 가능하다 (벌크 DELETE는 엔티티 필터 미적용).

---

## 6. CurrentUser 처리

- `@CurrentUser` 어노테이션에서 `CustomUserDetails`를 가져오는 방식으로 수정
- 기존 방식(User 엔티티 직접 저장)은 SecurityContextHolder에 엔티티를 그대로 저장하므로 엔티티 변경 시 잘못된 정보를 참조할 수 있음
- `CustomUserDetails`에는 `userId`만 저장, 서비스 로직에서 필요 시 User 엔티티 조회

---

## 7. 커밋 단위

| # | 커밋 메시지 | 대상 파일 |
|---|---|---|
| ① | `chore: #3 인증 관련 의존성 추가 (jjwt)` | `build.gradle.kts` |
| ② | `chore: #3 불필요한 설정 제거 및 환경 변수 정리` | `PasswordConfig.kt`, `R2Config.kt`, `R2Properties.kt` 삭제 / `application.yml`, `application-prod.yml`, `.env.example`, `docker-compose.yml`, `.gitignore`, `.dockerignore` |
| ③ | `chore: #3 GitHub 이슈 템플릿 및 라벨 추가` | `.github/ISSUE_TEMPLATE/*.yml`, `labels.json` |
| ④ | `feat: #3 User 엔티티 및 인증 도메인 기반 타입 추가` | `User.kt`, `UserStatus.kt`, `UserRole.kt`, `AuthProvider.kt`, `DeviceType.kt`, `AuthErrorCode.kt` |
| ⑤ | `feat: #3 JwtProvider 구현` | `JwtProvider.kt`, `HashUtils.kt`, `JwtProperties.kt` |
| ⑥ | `refactor: #3 보안 관련 파일 global → domain/auth 패키지로 이동` | `CustomUserDetails`, `CustomAccessDeniedHandler`, `CustomAuthenticationEntryPoint`, `SecurityErrorResponseWriter` 이동 / global 하위 구파일 삭제 |
| ⑦ | `feat: #3 JWT 인증 필터 및 예외 필터 구현` | `JwtAuthenticationFilter.kt`, `JwtExceptionFilter.kt`, `WebSecurityConfig.kt` |
| ⑧ | `feat: #3 AuthAccount 엔티티 및 레포지터리 추가` | `AuthAccount.kt`, `AuthAccountRepository.kt`, `UserRepository.kt` |
| ⑨ | `feat: #3 카카오 OIDC ID Token 검증 구현` | `oidc/` 패키지 전체, `JwksClient.kt`, `JwksResponseDTO.kt`, `OidcClaims.kt`, `KakaoProperties.kt` |
| ⑩ | `feat: #3 소셜 로그인 및 계정 연결 서비스 구현` | `OAuthService.kt`, `OidcLoginRequestDTO.kt`, `AuthTokenResponseDTO.kt` |
| ⑪ | `feat: #3 DB 기반 RefreshToken 엔티티 및 레포지터리 추가` | `RefreshToken.kt`, `RefreshTokenRepository.kt` |
| ⑫ | `feat: #3 토큰 갱신(로테이션) 및 로그아웃 구현` | `AuthService.kt`, `RefreshTokenRequestDTO.kt` |
| ⑬ | `feat: #3 회원탈퇴(14일 유예) 및 계정 복구 구현` | `WithdrawService.kt`, `User.kt`, `UserRepository.kt`, `KakaoApiClient.kt` |
| ⑭ | `feat: #3 인증 API 컨트롤러 구현 및 dev-login prod 차단` | `AuthController.kt`, `AuthApi.kt`, `DevAuthController.kt`, `DevLoginRequestDTO.kt`, `MockUserInitializer.kt`, `CurrentUserArgumentResolver.kt` |
| ⑮ | `test: #3 인증 도메인 테스트 픽스처 추가` | `UserFixture.kt`, `RefreshTokenFixture.kt`, `AuthTokenFixture.kt` |
| ⑯ | `test: #3 AuthService 및 JwtAuthenticationFilter 단위 테스트 추가` | `AuthServiceTest.kt`, `JwtAuthenticationFilterTest.kt` |
| ⑰ | `test: #3 AuthController 및 DevAuthController 슬라이스 테스트 추가` | `AuthControllerTest.kt`, `DevAuthControllerTest.kt`, `AuthAccountRepositoryTest.kt`, `UserRepositoryTest.kt` |
