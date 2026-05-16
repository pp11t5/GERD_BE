# QueryDSL Checklist

## Before Introduce
- 정말 QueryDSL이 필요한 복잡도인가
- `@Query` 또는 파생 메서드로 충분하지 않은가

## Query Shape
- 동적 조건이 3개 이상인가
- 집계/그룹핑/정렬 전환이 필요한가
- 커서 페이지네이션과 tie-breaker가 필요한가
- 서브쿼리나 `not exists`가 필요한가

## Performance
- N+1 위험이 없는가
- to-one은 fetch join, 컬렉션은 2단계 조회가 더 적절한가
- projection으로 끝낼 수 있는 조회를 엔티티 로딩으로 만들고 있지 않은가
- 인덱스와 unique constraint 검토가 필요한가

## Tests
- 정렬 순서와 커서 조건이 맞는가
- `hasNext` 계산이 맞는가
- 집계 결과와 제외 조건을 검증하는가
