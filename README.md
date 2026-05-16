# gerd

# Prography 11 Backend 5 Team Handbook

백엔드 팀의 개발 규칙, 협업 방식, AI Agent 운영 기준을 관리하는 저장소입니다.

이 저장소는 실제 서비스 코드 저장소가 아니라,  
팀의 **공통 개발 기준(Single Source of Truth)** 을 관리하기 위한 저장소입니다.

---

## 목적

아래 내용을 팀 공통 기준으로 관리합니다.

- 개발 환경 및 기술 스택
- 브랜치 전략
- 코드 컨벤션
- API 응답 규칙
- 테스트 전략
- PR / 코드리뷰 방식
- 인프라 운영 원칙
- AI Coding Agent 사용 규칙
- 재사용 가능한 Skill / Prompt

---

## Docs

| 문서 | 설명 |
|---|---|
| [tech-stack.md](docs/tech-stack.md) | 언어·프레임워크·DB·보안·배포 기술 스택과 각 선택 근거 |
| [branch-strategy.md](docs/branch-strategy.md) | Git 브랜치 전략 및 PR 규칙 |
| [coding-convention.md](docs/coding-convention.md) | Kotlin/Spring 코드 스타일, 네이밍, 패키지 구조 |
| [api-response.md](docs/api-response.md) | ApiResponse 포맷, 성공/에러 코드 체계, traceId 흐름 |
| [test-strategy.md](docs/test-strategy.md) | 테스트 계층별 전략 및 작성 기준 |
| [deployment.md](docs/deployment.md) | Railway 배포 방식, CI/CD 파이프라인, 운영 전환 전략 |
| [dev-environment.md](docs/dev-environment.md) | local/test 환경 설정 및 초기 데이터 정책 |

---

## Skills

Claude Code에서 `/<skill>` 또는 `/develop`, `/code-review` 형태로 호출합니다.

| 스킬 | 경로 | 설명 |
|---|---|---|
| `api-design` | [.claude/skills/api-design/](/.claude/skills/api-design/SKILL.md) | API 응답 규칙, 예외 모델, DTO 설계 기준 |
| `spring-api` | [.claude/skills/spring-api/](/.claude/skills/spring-api/SKILL.md) | Controller/Api 인터페이스 분리, `@CurrentUser`, 응답 일관성 |
| `service-layer` | [.claude/skills/service-layer/](/.claude/skills/service-layer/SKILL.md) | 서비스 계층 책임, 트랜잭션 경계, CQRS 분리 기준 |
| `jpa-entity` | [.claude/skills/jpa-entity/](/.claude/skills/jpa-entity/SKILL.md) | 엔티티 생성/상태 변경, Kotlin JPA 선언 기준 |
| `querydsl` | [.claude/skills/querydsl/](/.claude/skills/querydsl/SKILL.md) | 동적 조건, 집계, 커서 페이지네이션, N+1 방지 |
| `patterns` | [.claude/skills/patterns/](/.claude/skills/patterns/SKILL.md) | 스케줄러, Coroutine, 이벤트 발행 등 아키텍처 패턴 도입 기준 |
| `comment-style` | [.claude/skills/comment-style/](/.claude/skills/comment-style/SKILL.md) | 한국어 단문 주석, "왜" 중심, 단계 경계 기준 작성 |
| `code-review` | [.claude/skills/code-review/](/.claude/skills/code-review/SKILL.md) | 위험도 중심 리뷰 — 버그, 보안, 동시성, 테스트 누락 탐지 |
| `refactoring` | [.claude/skills/refactoring/](/.claude/skills/refactoring/SKILL.md) | 기능 변경 없는 구조 개선, 책임 분리, 명명 개선 |
| `security-check` | [.claude/skills/security-check/](/.claude/skills/security-check/SKILL.md) | 인증·인가·입력 검증·민감정보·토큰·업로드 보안 점검 |
| `performance-check` | [.claude/skills/performance-check/](/.claude/skills/performance-check/SKILL.md) | DB 접근, 캐시, 외부 호출, 병목 구간 성능 사전 점검 |
| `test-writing` | [.claude/skills/test-writing/](/.claude/skills/test-writing/SKILL.md) | Controller·Service·Repository·통합 테스트 작성 기준, fixture 패턴 |

---

## Shared Skills Sync

- 공용 skill 원본은 이 저장소 밖의 `../backend-core-skills` clone 입니다.
- `.claude/skills`, `.agents/skills` 는 위 외부 저장소 경로를 가리키는 symlink입니다.
- 최초 1회 clone:

```bash
git clone https://github.com/pp11t5/backend-core-skills.git ../backend-core-skills
```

- 최신 변경 반영:

```bash
git -C ../backend-core-skills pull origin main
```

---

## Repository Structure

```
.
├── README.md
├── AGENTS.md
├── docs/
│   ├── tech-stack.md
│   ├── branch-strategy.md
│   ├── coding-convention.md
│   ├── api-response.md
│   ├── test-strategy.md
│   └── deployment.md
|   └── dev-environment.md
└── .claude/
    └── skills/
        ├── api-design/
        ├── spring-api/
        ├── service-layer/
        ├── jpa-entity/
        ├── querydsl/
        ├── patterns/
        ├── comment-style/
        ├── code-review/
        ├── refactoring/
        ├── security-check/
        ├── performance-check/
        └── test-writing/
```
