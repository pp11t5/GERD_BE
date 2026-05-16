# Security Review Template

## Auth / Authz
- 인증 누락
- 인가 누락
- IDOR 가능성

## Input Boundary
- validation 누락
- 허용값 제한 누락
- 업로드 경로/MIME 검증 누락

## Sensitive Data
- 로그 노출
- 예외 노출
- 응답 DTO 노출

## State / Concurrency
- 재사용 토큰
- 실패 횟수 제한 누락
- 중복 생성/레이스 컨디션

## Tests
- 인증 성공/실패
- 권한 실패
- ErrorCode 검증
