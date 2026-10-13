# Git / PR 컨벤션

## 커밋 메시지
- Conventional Commits 형식, 한국어 설명: `<type>: <설명>` (예: `fix: 마이페이지 닉네임 변경 트랜잭션 미적용 수정`)
- 허용 타입: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
- 커밋/PR 어디에도 `Co-Authored-By` 트레일러를 절대 추가하지 않는다.

## PR 생성
- `gh pr create`로 PR을 만들 때는 기본 "## Summary / ## Test plan" 형식 대신 `.github/PULL_REQUEST_TEMPLATE.md`의 섹션 구조를 그대로 따라 본문을 작성한다:
  - `## 📄 작업 내용 요약`
  - `## 📎 Issue 번호`
  - `## ✅ 작업 목록`
  - `## 📝 기타 참고사항`
- 관련 이슈가 있으면 "Issue 번호" 섹션에 `closed #123` 형태로 연결한다.
- `develop` → `main` 릴리즈 머지 PR은 `.github/PULL_REQUEST_TEMPLATE/release.md` 구조를 따른다 (릴리즈 번호, 릴리즈 요약, 포함된 이슈/PR, 배포 체크리스트). 직전 릴리즈 태그에서 번호를 +1 해서 기재한다.
