# Dev Environment



## 1. 문서 목적
이 문서는 개발 및 테스트 환경에서 Mock 데이터를 초기화하는 방법과 목적을 설명한다.

## 2. Mock Data 초기화

개발 및 테스트 환경에서는 애플리케이션 시작 시 Mock 데이터를 자동으로 생성한다.

### 대상
- local
- test

### 방식
- `CommandLineRunner` 기반 초기화
- `@Profile("local", "test")`로 환경 제한

### 기본 데이터
- USER: dev-user@gerd.local
- ADMIN: dev-admin@gerd.local

### 목적
- 인증/인가 테스트 편의성 확보
- 반복적인 회원가입 과정 제거

### 주의사항
- production 환경에서는 실행되지 않음
- 동일 이메일 존재 시 생성하지 않음 (idempotent)