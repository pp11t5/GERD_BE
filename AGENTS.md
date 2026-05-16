# AGENTS.md

AI Coding Agent(Claude Code 등)가 이 저장소에서 작업할 때 따라야 할 기준을 정리한다.

---

## 스킬 참조 기준

이 저장소의 `.claude/skills/` 아래에 도메인별 스킬이 정의되어 있다.  
작업 종류에 따라 해당 스킬을 우선 참조한다.

| 작업 | 참조 스킬 |
|---|---|
| API 설계 | `api-design` |
| Controller 구현 | `spring-api` |
| Service 계층 | `service-layer` |
| JPA Entity | `jpa-entity` |
| QueryDSL 조회 | `querydsl` |
| 테스트 작성 | `test-writing` |
| 코드리뷰 | `code-review` |
| 리팩터링 | `refactoring` |
| 보안 점검 | `security-check` |
| 성능 점검 | `performance-check` |

---

## 기술 스택

- **Language**: Kotlin
- **JVM**: Java 21
- **Framework**: Spring Boot 4 (Spring Framework 6)
- **ORM**: Spring Data JPA + QueryDSL
- **DB**: H2 (local/test) / PostgreSQL (prod)
- **Build**: Gradle (Kotlin DSL)

---

## 작업 원칙

- 요청 범위 밖의 리팩터링, 의존성 추가, 주석 삽입은 하지 않는다
- 보안 취약점(SQL Injection, XSS 등)이 발생하는 코드는 즉시 수정한다
- 엔티티 생성/수정은 `jpa-entity` 스킬 기준을 따른다
- 응답 포맷은 `docs/api-response.md` 기준을 따른다
- 브랜치 전략은 `docs/branch-strategy.md` 기준을 따른다

---

## docs 참조

| 문서 | 내용 |
|---|---|
| `docs/tech-stack.md` | 기술 스택 선택 근거 |
| `docs/branch-strategy.md` | 브랜치 전략, PR 규칙 |
| `docs/coding-convention.md` | 코딩 컨벤션 |
| `docs/api-response.md` | API 응답 포맷, 예외 모델 |
| `docs/test-strategy.md` | 테스트 전략 |
| `docs/deployment.md` | 배포/운영 전략 |
