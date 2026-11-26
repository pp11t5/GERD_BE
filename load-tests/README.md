# k6 부하 테스트

staging이 기본 트래픽을 버티는지 확인하고 병목이 될 만한 API를 찾기 위한 스크립트 모음이다. 먼저 로컬에서 돌려 보고, 문제가 없으면 staging에 쓴다. prod에는 쓰지 않는다.

## 준비

```bash
brew install k6
```

대상 서버가 떠 있어야 한다. 로컬은 `./gradlew bootRun`으로 띄우고 DB 연결이 필요하다.

## 환경변수

| 이름 | 설명 | 기본값 |
|---|---|---|
| `BASE_URL` | 대상 서버 주소 | `http://localhost:8080` |
| `ACCESS_TOKEN` | 사용할 액세스 토큰. 있으면 `NICKNAMES`는 무시한다 | 없음 |
| `NICKNAMES` | `dev-login`으로 토큰을 받을 계정 닉네임, 쉼표로 구분 | 없음 |
| `P95_MS`, `P99_MS` | 응답 시간 임계값(ms) | `500`, `1000` |
| `TARGET_VUS` | 핵심 시나리오의 최대 동시 사용자 수 | `20` |
| `RATE` | 병목 테스트의 초당 요청 수 | `20` |
| `INCLUDE_JUDGMENT`, `FOOD_EXTERNAL_ID` | 음식 판정 API 포함 여부와 대상 음식 ID | 제외 |

인증은 둘 중 하나로 한다.
- 토큰을 직접 넘긴다: `ACCESS_TOKEN=... k6 run ...`. 액세스 토큰 수명이 30분이라 긴 테스트에서는 만료될 수 있다.
- `dev-login`으로 받는다: `NICKNAMES=qa_a,qa_b`. local, test, staging에서만 동작한다. 계정마다 VU가 하나씩 나눠 쓰므로 계정이 많을수록 실제 사용에 가깝다.

토큰은 저장소에 커밋하지 않는다.

## 실행

```bash
# 1. smoke: 서버와 인증이 정상인지 30초만 확인
BASE_URL=http://localhost:8080 NICKNAMES=qa_a k6 run load-tests/smoke.js

# 2. 핵심 사용자 시나리오: 홈, 검색, 타임라인, 마이페이지를 20 VU로 약 4분
BASE_URL=https://<staging-host> NICKNAMES=qa_a,qa_b,qa_c k6 run load-tests/core-scenario.js

# 3. 병목 후보 API: 초당 요청 수를 올리며 약 5분
BASE_URL=https://<staging-host> NICKNAMES=qa_a,qa_b,qa_c RATE=20 k6 run load-tests/bottleneck.js
```

결과를 파일로 남기려면 `--summary-export=load-tests/results/core.json`을 붙인다. `results/`는 git에서 제외된다.

## 시나리오

- `smoke.js`: 1 VU로 30초. 헬스체크와 `GET /auth/me`.
- `core-scenario.js`: 온보딩 상태, 홈 추천, 음식 검색, 최근 검색, 타임라인, 마이페이지 요약을 순서대로 호출한다. 1분 동안 올리고 3분 유지한 뒤 내린다.
- `bottleneck.js`: 도착률 기반이라 서버가 느려져도 요청이 줄지 않고 쌓인다. 음식 검색(40%), 월간 타임라인(20%), 사전 안전 음식(20%), 리포트(15%)를 섞고, 마지막 구간에서 2배로 올린다. 음식 판정 API는 LLM 호출 비용이 들 수 있어 기본에서 뺐다. 필요하면 `INCLUDE_JUDGMENT=true`와 `FOOD_EXTERNAL_ID`를 준다.

## 통과 기준(threshold)

| 지표 | 기준 | 기준 초과 시 |
|---|---|---|
| `http_req_duration` p95 | 500ms 미만 | 종료 코드 99로 실패 표시 |
| `http_req_duration` p99 | 1000ms 미만 | 종료 코드 99로 실패 표시 |
| `http_req_failed` | 1% 미만 | 종료 코드 99로 실패 표시 |
| `checks` | 99% 초과 | 종료 코드 99로 실패 표시 |

병목 테스트는 검색, 월간 타임라인, 사전, 리포트마다 p95 800ms 기준을 따로 둔다. 기준값은 임시이니 첫 결과를 보고 조정한다. 기준을 못 넘었다고 바로 장애는 아니고, 어느 API가 먼저 느려지는지 보는 용도다.

## DB 연결 문제 확인 기준

`application.yml`에 Hikari 설정이 없어서 기본값(풀 크기 10, 연결 대기 30초)을 쓴다. 부하가 풀 크기를 넘으면 요청이 연결을 기다리다 아래처럼 나타난다.

- 응답 시간이 갑자기 길어지거나, 임계값 근처에서 계단처럼 오른다.
- 서버 로그에 `Connection is not available, request timed out after 30000ms`가 찍히고 5xx가 늘어난다.
- Micrometer 메트릭 `hikaricp.connections.pending`이 계속 0보다 크거나, `hikaricp.connections.timeout`이 올라간다. 메트릭은 BetterStack에서 본다.
- 이런 징후가 나타난 부하 수준을 기록해 두고, 풀 크기와 쿼리를 손보는 기준으로 쓴다.

## 주의

- 모든 VU가 같은 계정을 쓰면 캐시가 잘 맞아서 실제보다 좋게 나온다. 계정을 여러 개 준비한다.
- staging DB에 쓰기를 하지 않는 읽기 위주 시나리오로만 구성했다. 쓰기 시나리오를 추가하면 데이터가 쌓이는 점에 주의한다.
- 앱에는 rate limit이 걸려 있다. 로그인(`/auth/*/login`)과 토큰 갱신(`/auth/refresh`)은 IP당 분당 20회, 관리자 로그인은 10분에 5회라서 스크립트에서 이 경로를 반복 호출하면 429가 난다. 이 스크립트는 `dev-login`만 처음에 한 번 호출한다.
- LLM을 부르는 음식 판정 API는 외부 비용과 예산 한도에 영향을 줄 수 있어 기본으로 제외했다.
- 로컬 결과는 운영 환경의 성능을 그대로 말해 주지 않는다. staging 수치와 따로 읽는다.
