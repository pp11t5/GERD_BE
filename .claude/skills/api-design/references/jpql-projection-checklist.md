# JPQL Projection Checklist

## Query Shape
- 단순 엔티티 조회인가
- 단순 scalar 조회인가
- 집계/동적 정렬/커서/서브쿼리가 필요한가

## DTO Strategy
- API 응답 DTO를 그대로 `select new`에 맞추고 있지 않은가
- 조회 전용 DTO를 따로 두는 게 더 맞는가
- service/converter 조립이 더 자연스러운가

## Escalation
- projection 필드가 많아졌는가
- `having`, 동적 정렬, 커서가 필요한가
- QueryDSL custom repository로 올리는 편이 더 읽기 쉬운가