# 관리자(Admin) 기능 설계 및 API 리스트업

> 상태: **설계 초안 (미구현)** · 이번 PR 범위 아님
> 작성 배경: 주간 리포트 일괄 생성의 "실패 유저 수동 재실행"을 계기로, 관리자 전용 기능 전반을 함께 설계.

---

## 0. TL;DR

- 지금 코드에 `ROLE_ADMIN` 분기(`UserRole.ADMIN`, JWT `role` claim, 시큐리티 `hasRole("ADMIN")`)는 있으나, **ADMIN 역할을 발급받을 경로가 없어 모든 관리자 API가 호출 불가(죽은 API)** 상태다.
- 따라서 **선결 조건은 "관리자 인증" 확보**다. 권장안은 소셜 로그인을 그대로 쓰되 특정 계정을 `role=ADMIN`으로 승격하는 방식(아래 1번).
- 관리자 API는 **모두 `/api/v1/admin/**` 하위**로 두고 `hasRole("ADMIN")`로 일괄 보호하며, Swagger는 `admin` 그룹으로 분리한다.

---

## 1. 선결 과제 — 관리자 인증

### 현재 상태

| 요소 | 위치 | 비고 |
|------|------|------|
| 역할 enum | `UserRole { USER, ADMIN }` | 이미 존재 |
| JWT role claim | `JwtProvider` (`role` claim 발급), `JwtAuthenticationFilter` (복원) | 이미 존재 |
| 시큐리티 권한 매핑 | `CustomUserDetails.getAuthorities()` → `ROLE_{role.name}` | 이미 존재 |
| 경로 보호 | `WebSecurityConfig`: `/api/v1/admin/**`, `/v3/api-docs/admin` → `hasRole("ADMIN")` | 매처는 있으나 **TODO: 관리자 로그인 미구현** |
| **역할 부여 경로** | — | **없음 (블로커)** |

### 권장안 — 소셜 로그인 + 역할 승격 (별도 로그인 X)

로그인 흐름(OIDC 소셜 / dev-login)은 그대로 두고, **특정 `User.role`만 `ADMIN`으로 지정**한다. JWT가 이미 role을 싣고 다니므로 추가 인증 흐름이 필요 없다.

승격 수단(택1, 운영 난이도 순):
1. **시드/마이그레이션** — 운영 DB에 지정 이메일 계정을 `role=ADMIN`으로 세팅 (가장 단순, 최초 1명 부트스트랩에 적합).
2. **슈퍼 관리자 승격 API** — 기존 ADMIN이 다른 유저를 승격 (`PATCH /api/v1/admin/users/{userId}/role`). 단, 최초 1명은 1번으로 만들어야 함(부트스트랩 문제).

> 보안 메모: 승격 API를 둔다면 "마지막 ADMIN 강등 금지", 감사 로그(누가 누구를 언제) 필수.

### 대안 — 별도 관리자 로그인 (비권장)

이메일+비밀번호 기반 별도 자격증명 도입. 자격증명 저장/해시/잠금 정책 등 신규 구축 비용이 큼. 현재 소셜 기반 구조와 이질적이라 **초기에는 비권장**.

### 함께 고칠 것 — Swagger 보호 순서 버그

`WebSecurityConfig.PUBLIC_URLS`의 `"/v3/api-docs/**"` permitAll이 `"/v3/api-docs/admin"` `hasRole` 매처보다 **먼저 등록**되어, 현재는 admin 문서가 실제로 보호되지 않는다. 관리자 인증 도입 시 **admin 매처를 permitAll 앞으로 이동**해야 한다.

```
auth
    .requestMatchers("/v3/api-docs/admin").hasRole("ADMIN")   // permitAll 보다 먼저
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    .requestMatchers(*PUBLIC_URLS).permitAll()
    .anyRequest().authenticated()
```

### Swagger 그룹 분리

`springdoc`의 `GroupedOpenApi` 빈 2개로 분리:
- `default` 그룹 — `/api/v1/**` 매칭, `/api/v1/admin/**` 제외 → `/v3/api-docs/default`
- `admin` 그룹 — `/api/v1/admin/**` 매칭 → `/v3/api-docs/admin` (ADMIN 보호)

---

## 2. 필요 API 리스트업

표기: **우선순위** P0(인증/이번 트리거) · P1(운영 필수) · P2(있으면 좋음)
모든 경로 prefix = `/api/v1/admin`, 권한 = `ROLE_ADMIN`.

### 2-1. 인증/권한 (P0)

| Method | Path | 설명 |
|--------|------|------|
| PATCH | `/users/{userId}/role` | 유저 역할 변경(USER↔ADMIN). 마지막 ADMIN 강등 금지 |

> 최초 ADMIN 1명은 API가 아닌 시드/마이그레이션으로 부트스트랩.

### 2-2. 리포트 운영 (P0 — 이번 트리거)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/reports/weekly/run` | 전체 유저 지난주 리포트 **수동 일괄 생성** (스케줄 외 재실행) |
| POST | `/reports/weekly/rerun` | **실패 유저만 재실행** — body `{ userIds: [Long] }`. `getOrCreate` 멱등이라 중복 생성 없음 |
| GET | `/reports/weekly?userId=&startDate=` | 특정 유저 리포트 조회(디버깅) |

> 구현 메모: 일괄/재실행 로직은 이미 있는 `ReportBatchProcessor`(keyset 페이징 + 유저별 트랜잭션 + 성공/실패 집계)를 재사용. 응답으로 성공/실패 건수·샘플 반환.

### 2-3. 회원 관리 (P1)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/users` | 회원 목록 — 페이징/검색(닉네임·이메일)/필터(status, role, 가입일) |
| GET | `/users/{userId}` | 회원 상세 — 프로필/복약/알러지/증상·끼니 요약 |
| PATCH | `/users/{userId}/status` | 상태 변경(ACTIVE/DELETED) — 강제 탈퇴/정지 |
| GET | `/users/withdrawn` | 탈퇴 유예(DELETED, 14일) 회원 목록 |
| POST | `/users/{userId}/restore` | 유예 중 계정 복구 |
| DELETE | `/users/{userId}` | 즉시 물리 삭제(`hardDelete`) — 유예 무시 |

### 2-4. 음식 마스터/사전 관리 (P1 — 서비스 핵심 데이터)

> 대상 엔티티: `Food`, `FoodCategory`, `Allergen`, `TriggerLabel`/`FoodTrigger`, `FoodSubstitute`, `UserFoodDictionary`(사전)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/foods` | 음식 목록/검색/페이징 |
| POST | `/foods` | 음식 등록 |
| PATCH | `/foods/{foodId}` | 음식 수정 |
| DELETE | `/foods/{foodId}` | 음식 비활성/삭제 |
| GET·POST·PATCH·DELETE | `/food-categories` | 카테고리 관리 |
| GET·POST·PATCH·DELETE | `/allergens` | 알러지 항목 관리 |
| GET·POST·PATCH·DELETE | `/trigger-labels` | 트리거 라벨 관리 |
| POST·DELETE | `/foods/{foodId}/triggers` | 음식-트리거 매핑 |
| POST·DELETE | `/foods/{foodId}/substitutes` | 대체 음식 매핑 |

### 2-5. 판정(Judgment) 운영 (P2)

| Method | Path | 설명 |
|--------|------|------|
| GET·POST·PATCH·DELETE | `/judgment/safety-rules` | 안전 오버라이드 규칙(`SafetyOverrideRule`) 관리 |
| GET | `/judgment/stats` | 판정 등급(RECOMMEND/CAUTION/RISK) 분포 통계 |
| DELETE | `/judgment/cache` | 판정 캐시(`JudgmentCache`) 무효화 |

### 2-6. 알림/푸시 (P2)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/notifications/push` | 수동 푸시 발송(전체/세그먼트) |
| GET | `/notifications/pending` | 발송 대기(`NotificationPending`) 조회 |
| GET | `/notifications/stats` | 알림 설정/수신 동의 통계 |

### 2-7. 통계 대시보드 (P2)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/dashboard/overview` | 가입자수·주간 활성 유저·끼니/증상 기록수·판정 등급 분포 |
| GET | `/dashboard/scheduler-history` | 스케줄러 실행 이력(리포트 생성, hardDelete 등) |

---

## 3. 구현 순서(제안)

1. **P0-A 관리자 인증** — 시드로 ADMIN 1명 부트스트랩 + Swagger 보호 순서 교정 + `admin` 그룹 분리
2. **P0-B 리포트 운영** — `run` / `rerun` / 조회 (이번 트리거, `ReportBatchProcessor` 재사용)
3. **P1 회원 관리 → 음식 마스터 관리**
4. **P2 판정/알림/대시보드**

> 1번이 끝나기 전에는 2번 이후가 모두 호출 불가하므로, **반드시 인증부터** 진행.

---

## 4. 미해결/결정 필요

- 역할 승격을 시드만으로 갈지, 승격 API까지 둘지
- 음식 마스터 관리를 관리자 API로 노출할지(DB/배치 운영으로 갈지)
- 감사 로그(관리자 행위 기록) 도입 범위
- 대시보드 통계의 집계 방식(실시간 쿼리 vs 사전 집계)
