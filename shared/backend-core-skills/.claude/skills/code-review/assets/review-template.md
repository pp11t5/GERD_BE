# Code Review Template

## Findings
- [Critical] `path/to/file:line` - 문제와 영향
- [High] `path/to/file:line` - 문제와 영향
- [Medium] `path/to/file:line` - 문제와 영향

## Open Questions
- 확인이 필요한 가정

## Residual Risks
- 테스트 공백
- 운영 리스크

## Quick Passes
- API 계약/Validation/에러 응답
- Controller 책임 침범 여부
- Service 트랜잭션/동시성/예외
- Entity 생성/상태 변경 규칙
- Repository/QueryDSL/N+1/인덱스
- 보안/민감정보/인가
- 테스트와 회귀 방지
