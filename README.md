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
│   ├── deployment.md
│   └── dev-environment.md
└── src/
```
