# Custom Repository Checklist

## Before Introduce
- 기본 `JpaRepository` 또는 `@Query`로 충분하지 않은가
- 커서, 집계, 복수 조건, projection이 필요한가

## Structure
- 기본 Repository와 custom interface가 분리되는가
- 구현체 이름이 `...RepositoryImpl` 규칙을 따르는가

## Return Type
- 엔티티가 필요한가 DTO면 충분한가
- `Slice<T>` 또는 `size + 1` 패턴이 필요한가

## Query Strategy
- QueryDSL 도입 이유가 명확한가
- fetch join과 2단계 조회 중 무엇이 맞는가
- N+1 없이 설명 가능한가

## Review Points
- 메서드 이름이 도메인 의미를 드러내는가
- projection 메서드와 엔티티 반환 메서드가 섞이지 않는가
- 서비스가 `EntityManager`를 직접 쓰지 않는가
