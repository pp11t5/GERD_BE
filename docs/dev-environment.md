# Dev Environment

## 1. 문서 목적
이 문서는 개발·검증 환경에서 QA 계정을 다루는 기준을 설명한다.

## 2. QA 계정 생성 기준

애플리케이션 기동 시 Mock 사용자를 자동 생성하지 않는다.

### 권장 방식
- local/test/staging에서 필요한 QA 계정은 SQL 또는 별도 1회성 관리 스크립트로 생성한다
- staging 계정은 repo, Swagger 예시, 기본 설정 파일에 실제 로그인 가능한 값으로 남기지 않는다
- 온보딩 전/후 상태가 모두 필요하면 각각 별도 계정으로 생성한다

### 예시

```sql
INSERT INTO users (email, nickname, role, status, created_at, updated_at)
VALUES ('qa-front-01@staging.internal', 'qa_front_01', 'USER', 'ACTIVE', now(), now());
```

```sql
INSERT INTO user_profiles (user_id, custom_trigger_text, onboarded_at, created_at, updated_at)
SELECT user_id, 'staging qa', now(), now(), now()
FROM users
WHERE nickname = 'qa_front_01';
```

### 목적
- 인증/인가 테스트 편의성 확보
- 반복적인 회원가입 과정 제거
- 온보딩 전/후 계정 상태를 분리해 QA 가능

### 주의사항
- production에는 QA 계정을 생성하지 않는다
- staging에서 개발용 로그인 API를 열 경우 별도 secret header 같은 보호 장치를 함께 둔다
