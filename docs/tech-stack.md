# 기술 스택 정리

## 1. 문서 목적
이 문서는 현재 프로젝트의 기술 스택과 각 선택 근거를 정리한다.  
기준 파일: `build.gradle.kts`

---

## 2. 언어 / 런타임

| 항목 | 선택 | 버전 |
|---|---|---|
| Language | Kotlin | 2.2.21 |
| JVM | Java | 21 (LTS) |
| Build | Gradle (Kotlin DSL) | 9.4.1 |

**Kotlin 컴파일러 옵션**
- `-Xjsr305=strict`: Spring의 `@NonNull`/`@Nullable` 어노테이션을 Kotlin null 타입으로 강제 처리
- `-Xannotation-default-target=param-property`: 생성자 파라미터에 어노테이션 자동 전달 (Bean Validation 호환)

---

## 3. 프레임워크

| 항목 | 선택 | 버전 |
|---|---|---|
| Framework | Spring Boot | 4.0.5 |
| Web | Spring MVC (`spring-boot-starter-webmvc`) | — |
| Security | Spring Security | — |
| Validation | Spring Validation (Jakarta Bean Validation) | — |
| Monitoring | Spring Actuator | — |

**Spring Boot 4 선택 근거**
- Spring Framework 7 기반, Virtual Thread 지원, Jakarta EE 11
- 생태계 성숙도가 높아 인증·DB·검증·배포까지 표준 패턴이 풍부함
- Controller-Service-Repository 계층 구조로 팀 협업과 유지보수성 확보

---

## 4. 데이터베이스

| 항목 | 선택 | 비고 |
|---|---|---|
| ORM | Spring Data JPA + Hibernate | — |
| 동적 쿼리 | QueryDSL 5.0 (Jakarta) | 복잡 조건 조회 전용 |
| 로컬 DB | H2 (in-memory, MODE=PostgreSQL) | 기본 `application.yml` |
| 테스트 DB | H2 (in-memory, MODE=PostgreSQL) | `application-test.yml` |
| 운영 DB | PostgreSQL | `prod` 프로파일 |

**QueryDSL 도입 기준**
- 동적 조건 3개 이상 조합, 복잡 정렬·페이징·조인이 필요한 경우에 한해 사용
- 단순 단건/고정 조건 조회는 Spring Data JPA 메서드 또는 `@Query`로 처리

**H2 주의사항**
- `MODE=PostgreSQL` 설정으로 차이를 줄이지만 100% 동일하지 않음
- 운영과 다른 동작이 의심될 경우 로컬에서 PostgreSQL 직접 구동 후 검증
- 저장소의 `docker-compose.yml`은 PostgreSQL/H2 컨테이너를 띄우기 위한 보조 수단이며, 현재 기본 실행/테스트 설정과 자동 연결되지는 않음

---

## 5. 보안 / 인증

| 항목 | 선택 | 버전 |
|---|---|---|
| 인증 | JWT (JJWT) | 0.11.5 |
| 보안 필터 | Spring Security | — |

**인증 엔드포인트**

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/auth/{provider}/login` | OIDC ID Token 검증 후 신규 가입 또는 로그인 |
| `POST` | `/api/v1/auth/refresh` | Refresh Token 로테이션으로 토큰 재발급 |
| `DELETE` | `/api/v1/auth/logout` | Refresh Token 삭제 |
| `DELETE` | `/api/v1/auth/withdraw` | 상태를 `DELETED`로 변경, 전 기기 Refresh Token 즉시 만료 |
| `POST` | `/api/v1/auth/{provider}/recover` | 탈퇴 유예(14일) 중 계정 복구 |
| `POST` | `/api/v1/auth/dev-login` | 닉네임으로 토큰 발급 (dev/local 전용) |

**JWT 구조**
- Access Token 만료 1h / Refresh Token 만료 3d, claims 기반 인증으로 매 요청 DB 조회 없음
- `JwtProvider`: Access/Refresh Token 생성·검증·claims 추출
- `RefreshToken`: userId를 PK로 DB에 `sha256(tokenValue)` 저장 — save()가 upsert로 동작해 단일 세션 유지
- `JwtAuthenticationFilter`: Bearer 토큰 파싱 → `SecurityContextHolder` 세팅
- `@CurrentUser`: `HandlerMethodArgumentResolver`로 Controller에서 `CustomUserDetails` 주입 (userId만 저장, 서비스에서 필요 시 User 엔티티 조회)

---

## 6. API 문서

| 항목 | 선택 | 버전 |
|---|---|---|
| API 문서 | SpringDoc OpenAPI (Swagger UI) | 2.8.5 |

- `[Domain]Api` 인터페이스에 `@Operation`, `@ApiResponses` 작성
- Controller 구현체에는 Swagger 어노테이션 작성하지 않음
- 공통 에러 응답 스키마(`400`, `401`, `403`, `404`, `409`, `500`)는 `SwaggerConfig`에서 일괄 등록

---

## 7. 파일 스토리지

| 항목 | 선택 | 버전 |
|---|---|---|
| Object Storage | Cloudflare R2 | AWS SDK v2 (2.25.4) |

- R2는 S3 호환 API → AWS SDK v2 (`software.amazon.awssdk:s3`) 그대로 사용
- `R2Config`, `R2Properties` 설정 클래스는 현재 미사용으로 제거됨 (파일 스토리지 도메인 구현 시 재추가 예정)
- 자격증명: `R2_ACCESS_KEY`, `R2_SECRET_KEY`, `R2_ENDPOINT`, `R2_BUCKET` 환경변수로 주입 예정

---

## 8. 모니터링 / 에러 추적

| 항목 | 선택 | 버전 |
|---|---|---|
| 에러 추적 | Sentry | 7.9.0 |
| 헬스체크 | Spring Actuator (`/actuator/health`) | — |
| 요청 추적 | MDC TraceId (`X-Trace-Id` 헤더) | 자체 구현 |
| 운영 로그 | Logback JSON 로그 | logstash-logback-encoder 8.0 |

- Sentry: `prod` 프로파일에서만 활성화 (`SENTRY_DSN` 환경변수)
- Sentry: `environment`, `release`, `tracesSampleRate`를 환경변수로 주입
- TraceId: 요청마다 UUID 생성 → MDC 저장 → 실패 응답 바디 + 응답 헤더에 포함
- Actuator: `health`, `liveness`, `readiness`, `diskspace`, `ping` 기반 운영 상태 점검

---

## 9. 테스트

| 항목 | 선택 |
|---|---|
| 단위 테스트 | JUnit5 + Mockito |
| 컨트롤러 테스트 | `@WebMvcTest` + `spring-security-test` |
| JPA 계층 테스트 | `@DataJpaTest` + H2 |
| 테스트 Security | `TestSecurityConfig` (`@Import`로 JWT 필터 우회) |

---

## 10. 배포 / 인프라

| 항목 | 선택 |
|---|---|
| 컨테이너 | Docker (멀티스테이지 빌드, `gradle:9.4.1-jdk21` → `eclipse-temurin:21-jre`) |
| 초기 배포 | Railway |
| 확장 후보 | AWS ECS (Fargate) + RDS PostgreSQL |
| CI | GitHub Actions (`ci.yml` — `main`, `develop` PR 시 빌드 + 테스트) |

**Railway 운영 기준**
- `Dockerfile` 기반 배포 (`git push` → 자동 빌드/배포)
- 환경변수는 Railway Variables에서만 관리 (Git 저장 금지)
- PostgreSQL은 Railway PostgreSQL 서비스로 연결 (같은 프로젝트 내)
- `SPRING_PROFILES_ACTIVE=prod` 필수 설정
- 배포 후 `/actuator/health` 자동 헬스체크

**Railway 필수 환경변수**

| 변수 | 설명 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` 고정 |
| `JWT_SECRET` | JWT Access Token 서명 키 |
| `JWT_EXPIRATION_MS` | Access Token 만료 시간 (ms, 기본 3600000 = 1h) |
| `PGHOST` / `PGPORT` / `PGDATABASE` / `PGUSER` / `PGPASSWORD` | PostgreSQL 연결 정보 |
| `KAKAO_NATIVE_APP_KEY` | 카카오 네이티브 앱 키 (OIDC audience 검증) |
| `KAKAO_ISS` | 카카오 OIDC issuer URL |
| `KAKAO_JWKS_URL` | 카카오 JWKS 공개키 엔드포인트 |
| `KAKAO_ADMIN_KEY` | 카카오 Admin 키 (Unlink API용) |
| `SENTRY_DSN` / `SENTRY_ENVIRONMENT` / `SENTRY_RELEASE` / `SENTRY_TRACES_SAMPLE_RATE` | Sentry 에러 추적 및 트레이싱 제어 |

---

## 11. 프로파일 전략

| 프로파일         | DB | Sentry | JWT 기본값 |
|--------------|---|---|---|
| `local` (로컬) | H2 in-memory | 비활성 | 플레이스홀더 허용 |
| `test`       | H2 in-memory | 비활성 | 테스트 전용 고정값 사용 |
| `prod`       | PostgreSQL | 활성 | 환경변수 필수 (기본값 없음) |
