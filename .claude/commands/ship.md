---
description: 현재 브랜치의 변경을 커밋·푸시하고 PR 템플릿에 맞춰 PR 생성 (PR 생성 단계만 사용자 확인)
argument-hint: "[PR 대상 브랜치, 기본 develop]"
allowed-tools: Bash(git status *), Bash(git diff *), Bash(git log *), Bash(git add *), Bash(git commit *), Bash(git push *), Bash(gh pr create *), Bash(gh pr list *)
---

현재 브랜치의 작업을 올린다. 커밋과 푸시는 확인 없이 진행하고, `gh pr create`만 사용자 확인을 받는다.

## 절차
1. `git status`, `git diff`로 이번 작업에 해당하는 파일만 가려낸다. 무관한 미커밋 변경이 섞여 있으면 `git add -A`를 쓰지 말고 파일 경로를 하나씩 지정해 스테이징한다.
2. 커밋 메시지는 `<type>: <한국어 설명>` 형식으로 작성한다. 허용 타입은 feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert. 공동 작성자 트레일러는 넣지 않는다.
3. `git push -u origin <현재 브랜치>`로 푸시한다. main, develop에는 직접 푸시하지 않는다.
4. `.github/PULL_REQUEST_TEMPLATE.md`의 섹션 구조 그대로 PR 본문을 작성해 `gh pr create`를 실행한다.
   - `## 📄 작업 내용 요약`
   - `## 📎 Issue 번호` (관련 이슈가 있으면 `closed #123`, 없으면 "없음")
   - `## ✅ 작업 목록` (체크박스)
   - `## 📝 기타 참고사항`
   - 본문 불릿은 명사형이 아니라 ~했습니다체로 쓴다.
   - 대상 브랜치는 인자가 없으면 `develop`. develop → main 릴리즈 PR이면 `.github/PULL_REQUEST_TEMPLATE/release.md` 구조를 따르고 직전 릴리즈 태그에서 번호를 +1 한다.
5. PR이 만들어지면 URL을 알려준다.

인자: $ARGUMENTS
