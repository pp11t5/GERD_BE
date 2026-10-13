# Claude Code 하네스 구조 (`.claude/hooks`)

## 왜 만들었나

"에이전트 = 모델 + 하네스"라는 관점으로 보면, 지금까지는 `CLAUDE.md`나 메모리 파일 같은 가이드 계층만 있고 그걸 실제로 강제하는 장치가 거의 없었다. `feedback_dto_no_touch` 같은 규칙도 결국 대화 맥락 안에서 Claude가 "기억"해야만 지켜지는 거라, 컨텍스트가 길어지거나 요약되면 씹힐 위험이 있다. 그래서 반복적으로 걸리는 규칙들을 훅으로 옮겨서 실제로 차단·확인·기록되게 만들었다. 아이디어는 [harness-engineering 6계층 글](https://theaxlabs.com/blog/harness-engineering-6-layer-guide)에서 가져왔고, 이 저장소 기준으로 계층을 매핑하면 대략 이렇게 된다.

| 계층 | 이 저장소에서의 구현 |
|---|---|
| 가이드 | `CLAUDE.md`, memory 폴더 |
| 센서 | `ktlint-sensor.py` |
| 에이전틱 루프 | 아직 없음 (대화형 워크플로우라 우선순위 낮게 봄) |
| 메모리 | memory 폴더 (작업 상태 추적용 스크래치패드는 별도로 없음) |
| 권한/예산 | `settings.local.json` allow/ask, `dto-guard.py`, `dangerous-command-guard.py` |
| 관찰가능성 | `decisions.log`, `decisions-report.py` (세션 시작 때 반복 규칙·위험 명령 경보) |

## 파일 구조

```
.claude/
  hooks/
    _hook_common.py            # deny/ask/warn 공용 헬퍼 + 로깅
    git-commit-guard.py        # 커밋/PR 컨벤션
    dto-guard.py                # DTO 파일 편집 가드
    dangerous-command-guard.py  # 위험 bash 명령 차단
    ktlint-sensor.py            # Kotlin 편집 직후 린트
    decisions.log                # 런타임 로그 (gitignore됨, 커밋 안 함)
  settings.local.json           # 훅 등록 + 권한 allow/ask
```

### `_hook_common.py`

다른 훅들이 공통으로 쓰는 `deny()`, `ask()`, `warn()`을 모아둔 헬퍼다. 셋 다 하는 일 자체는 비슷한데, `permissionDecision`을 deny/ask 중 뭘로 낼지, 아니면 그냥 시스템 메시지만 띄울지가 다르다. 호출할 때마다 `decisions.log`에 JSON 한 줄씩 남기는데, 이게 지금까지는 없던 관찰가능성 계층 역할을 한다 — 어떤 규칙이 언제, 왜 걸렸는지 나중에 되짚어볼 수 있게. 로그 쓰기가 실패해도 훅 자체는 절대 안 죽게 `try/except`로 감싸놨다.

### `git-commit-guard.py`

원래도 있던 훅인데 이번에 두 가지를 바꿨다.

- 커밋 메시지가 conventional commit 형식(`fix: 설명` 같은)이 아니면 이제는 경고만 하는 게 아니라 아예 `deny`로 막는다. PR 본문 섹션 누락은 그대로 `warn`만 하도록 남겨뒀다 — PR 초안은 작성 중에 순서가 안 맞을 수도 있어서 유연하게 두는 게 나아 보였다.
- co-author 트레일러 금지는 원래 하던 대로 deny.

하나 고친 버그가 있는데, `settings.local.json`의 `"if": "Bash(git commit *)"` 조건이 멀티라인이나 `if`/`for` 같은 제어문이 섞인 bash 명령에서는 제대로 안 걸러지는 걸 실제로 겪었다. `git commit`이랑 전혀 상관없는 평범한 반복문 하나가 이 훅에 걸려서 막힌 적이 있다. 그래서 이제는 설정의 `if` 게이트만 믿지 않고, 스크립트 안에서도 명령어에 진짜 `git commit`/`gh pr create`가 들어있는지 한 번 더 확인한다.

### `dto-guard.py`

DTO 파일은 원래부터 "사용자 명시적 지시 없이 건드리지 말 것"이라는 규칙이 메모리에 있었는데, 이걸 훅으로 승격했다. DTO를 어떻게 구분할지 고민이 좀 있었는데, 파일명 접미사(`*RequestDTO.kt`, `*ResponseDTO.kt`)로 판단하면 오탐이 난다 — `CachedJudgment.kt`나 `JudgmentContext.kt`처럼 dto 폴더 안에 있어도 이름이 저 규칙을 안 따르는 파일도 있고, 반대로 `global/ai/LlmRequest.kt`처럼 이름은 `*Request.kt`인데 dto 폴더 밖에 있는 것도 있다. 그래서 파일명 대신 경로에 `/dto/` 세그먼트가 들어있는지로 판단하기로 했다.

동작은 `deny`가 아니라 `ask`다. 완전히 막아버리면 사용자가 진짜로 DTO를 고쳐달라고 할 때도 매번 막혀버리는데, 훅은 대화 맥락을 못 보니까 "지금 이게 명시적 요청인지 아닌지" 스스로 판단할 방법이 없다. 그래서 매번 사람한테 승인 프롬프트를 띄우는 쪽으로 타협했다 — 사용자가 정말 시킨 거면 그 자리에서 승인하면 그만이고, Claude가 혼자 판단해서 건드리려던 거면 그 자리에서 멈춘다.

### `dangerous-command-guard.py`

되돌리기 어려운 명령 네 가지를 막는다: `git push --force`(`--force-with-lease`는 예외로 통과시킨다, 이건 상대적으로 안전한 강제 푸시라서), `git reset --hard`, `git clean -f` 계열, `rm -rf` 계열. 정규식으로 명령 문자열을 훑는 방식이라 완벽하진 않다 — 예를 들어 이 문구들이 테스트 스크립트 안에 리터럴 문자열로만 들어 있어도 걸릴 수 있다(실제로 검증하다가 한 번 겪었다). 보안 경계로 만든 게 아니라 실수로 잘못 치는 걸 막으려는 용도라 이 정도 오탐은 감수하기로 했다.

### `ktlint-sensor.py`

Kotlin 파일을 Edit/Write/MultiEdit으로 건드린 직후(`PostToolUse`)마다 그 파일 하나만 `ktlint --relative`로 검사한다. 위반이 있으면 exit code 2로 끝내는데, 이러면 stderr 내용이 Claude한테 그대로 피드백으로 들어가서 스스로 알아채고 고칠 기회가 생긴다. exit 0이면 그냥 지나간다.

ktlint는 이 프로젝트에 원래 없었다. Gradle 플러그인도 없고 standalone CLI도 없어서, Homebrew로 로컬 머신에만 설치했다(`brew install ktlint`, 1.8.0). `build.gradle.kts`에는 일부러 안 넣었다 — 팀 전체가 쓰는 공식 빌드 도구로 만들지, 아니면 Claude 하네스 전용으로만 둘지는 아직 결정 안 된 사안이라서, 일단은 후자로 좁혀뒀다. 다른 머신에서 이 훅이 안 먹힌다면 ktlint가 그 머신에 없어서일 가능성이 크다.

## `settings.local.json` 구조

`PreToolUse`에 두 개 matcher가 있다. 하나는 `"Bash"`용이고 `git-commit-guard.py`(commit/pr 두 모드)랑 `dangerous-command-guard.py`가 걸려있다. 다른 하나는 `"Edit|Write|MultiEdit"`용이고 `dto-guard.py`가 걸려있다. `PostToolUse`는 `"Edit|Write|MultiEdit"` matcher 하나에 `ktlint-sensor.py`가 걸려있다.

## 남은 것

에이전틱 루프(재시도 횟수, 타임아웃, 예산 상한 같은 것)는 아직 없다. 관찰가능성 쪽은 `decisions-report.py`가 로그를 집계하고 세션 시작 때 경보를 띄우는 데까지 만들었지만, 실시간 알림이나 대시보드는 없다. 지금은 대화형으로 세션 하나 안에서 작업이 끝나는 흐름이라 우선순위가 낮다고 봤는데, 나중에 자동화 루프(`/loop`, cron 등)를 더 많이 쓰게 되면 다시 볼 만하다.
