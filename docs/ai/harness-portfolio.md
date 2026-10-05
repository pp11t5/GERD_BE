# Claude Code 하네스 (포트폴리오 요약)

## 한 줄 요약

AI 코딩 에이전트가 이 저장소에서 실수하지 않도록 규칙을 "기억"에서 "강제"로 옮겼고, 커밋부터 PR 생성까지의 흐름을 자동화하면서 모든 판정을 로그로 남겨 점검할 수 있게 만들었다.

## 문제

`CLAUDE.md`와 메모리 파일에 규칙을 적어 두면 Claude가 그걸 기억하고 있을 때만 지켜진다. 대화가 길어지거나 요약되면 DTO 수정 금지나 커밋 컨벤션 같은 규칙이 쉽게 빠진다. 규칙 위반을 사람이 뒤늦게 발견하는 일이 반복됐다.

## 해결: 훅으로 강제하고 로그로 관측

| 구성 | 하는 일 | 방식 |
|---|---|---|
| `git-commit-guard.py` | 커밋 메시지가 conventional commit이 아니거나 공동 작성자 트레일러가 있으면 차단, PR 본문에 템플릿 섹션이 빠지면 경고 | PreToolUse, deny/warn |
| `dto-guard.py` | `/dto/` 경로 파일 편집 시 사람에게 승인 요청 | PreToolUse, ask |
| `dangerous-command-guard.py` | `git push --force`, `git reset --hard`, `git clean -f`, `rm -rf` 차단 | PreToolUse, deny |
| `ktlint-sensor.py` | Kotlin 파일 편집 직후 해당 파일만 린트하고 위반을 Claude에게 되돌려 줌 | PostToolUse, exit 2 |
| `decisions.log` | 훅이 내린 모든 판정을 JSON 한 줄로 기록 | 1MB 넘으면 로테이션, 사유 300자 제한 |
| `decisions-report.py` | 로그를 집계하고, 같은 규칙이 반복되거나 위험 명령이 막히면 세션 시작 때 경보 | SessionStart, `/hook-report` |
| `/ship` | 이번 작업 파일만 골라 커밋하고 푸시한 뒤 PR 템플릿 구조로 PR 생성 | 커밋·푸시는 자동, PR 생성만 사람이 확인 |

## 설계 판단

- **DTO 가드는 deny가 아니라 ask로 뒀다.** 훅은 대화 맥락을 못 봐서 지금 수정이 사용자의 명시적 요청인지 알 수 없다. 완전히 막으면 정상 요청도 매번 막히므로, 사람이 그 자리에서 승인하거나 멈추게 했다.
- **DTO 판별은 파일명이 아니라 경로로 했다.** 파일명 접미사로 판단하면 `CachedJudgment.kt`는 빠지고 dto 폴더 밖의 `LlmRequest.kt`는 걸리는 오탐이 났다.
- **PR 섹션 누락은 경고만, 커밋 형식 위반은 차단으로 나눴다.** PR 초안은 작성 중에 순서가 어긋날 수 있어서다.
- **커밋·푸시는 자동, PR 생성만 확인으로 열어 뒀다.** 외부에 공개되는 시점에만 사람이 내용을 본다.
- **ktlint는 로컬에만 뒀다.** 팀 공식 빌드 도구로 올릴지 결정되지 않아 `build.gradle.kts`에는 넣지 않았다.

## 실제로 겪은 문제와 수정

- `settings.local.json`의 `if: Bash(git commit *)` 조건이 멀티라인 명령이나 제어문이 섞인 명령에서 제대로 걸러지지 않았다. 커밋과 무관한 반복문이 훅에 걸려 막혔다. 지금은 스크립트 안에서도 명령에 실제 `git commit`이나 `gh pr create`가 있는지 한 번 더 확인한다.
- `dangerous-command-guard`가 리터럴 문자열에도 반응한다. 로그를 보면 `rm -rf`가 없는 docker psql 명령이 막힌 기록이 있다. 실수 방지용이라 어느 정도의 오탐은 감수했지만, 패턴을 좁혀야 할 후보로 남아 있다.
- `/ship` 문서를 만들다가 문서 안에 적은 공동 작성자 트레일러 문구에 커밋 가드가 반응해 막혔다. 가드가 의도대로 동작한 사례이고, 문서에서는 문구를 풀어 써서 해결했다.

## 지금까지의 기록 (decisions.log, 2026-10-03 기준)

전체 34건이다.

| 훅 | 결정 | 건수 |
|---|---|---|
| commit-guard | deny | 8 |
| commit-guard | warn | 4 |
| dangerous-command-guard | deny | 9 |
| dto-guard | ask | 1 |
| ktlint-sensor | flag | 12 |

가장 많이 걸린 사유는 ktlint 위반 12회, `rm -rf` 차단 6회, 공동 작성자 트레일러 5회 순이다. 이 숫자는 한 사람이 몇 주 쓴 로컬 로그라서 효과를 증명하는 수치가 아니라 훅이 실제로 동작했다는 기록으로 읽어야 한다.

## 6계층 매핑

| 계층 | 구현 |
|---|---|
| 가이드 | `CLAUDE.md`, memory 폴더 |
| 센서 | `ktlint-sensor.py` |
| 에이전틱 루프 | 아직 없음 |
| 메모리 | memory 폴더 |
| 권한/예산 | `settings.local.json` allow/ask, `dto-guard.py`, `dangerous-command-guard.py` |
| 관찰가능성 | `decisions.log`, `decisions-report.py` |

## 한계와 다음 단계

- 경보는 세션 시작 때 한 번 뜬다. 실시간 알림이나 대시보드는 없다.
- 에이전틱 루프(재시도 횟수, 타임아웃, 예산 상한)는 없다. 자동화 루프를 더 쓰게 되면 다시 본다.
- 훅의 효과를 비교할 기준선이 없다. 훅이 없을 때의 위반 횟수를 따로 기록하지 않았다.
- 상세 설계는 [claude-hooks-harness.md](../claude-hooks-harness.md)에 있다.
