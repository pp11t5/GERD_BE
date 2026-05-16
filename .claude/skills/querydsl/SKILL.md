---
name: querydsl
description: Kotlin Spring 백엔드에서 QueryDSL 조회 구현 기준을 정리한 스킬. 동적 조건, 집계, 커서 페이지네이션, fetch join, projection, N+1 방지 규칙을 포함한다.
---

# QueryDSL

## Purpose

이 스킬은 현재 프로젝트의 QueryDSL 구현 방식에 맞춰 조회를 설계하고 구현할 때 사용한다.
일반론보다 지금 코드베이스에서 실제로 읽히는 패턴인 custom repository 분리, 메서드 단위 조건식 분리, `switch` 기반 정렬/커서 분기, `size + 1`, 2단계 조회 + 순서 복원을 우선한다.
기본 Repository, `@Query`, custom repository, QueryDSL 중 어디에 둘지 먼저 판단한 뒤 시작한다.

## 1) 언제 QueryDSL을 쓰는가

- 기본은 Spring Data JPA 파생 메서드 또는 `@Query`로 시작한다.
- 아래 중 하나 이상이면 QueryDSL 도입을 검토한다.
  - 동적 조건 조합이 3개 이상으로 늘어난다.
  - 집계, 그룹핑, 정렬 기준 전환이 함께 필요하다.
  - 커서 기반 페이지네이션에서 tie-breaker가 필요하다.
  - `not exists`, 서브쿼리, 존재 여부 기반 필터가 필요하다.
  - fetch join과 조회 조건을 같이 제어해야 한다.
- 단순 단건 조회, 고정 조건 조회, 단순 count 조회만으로 끝나는 경우는 QueryDSL로 과설계하지 않는다.

## 1-1) Repository 선택 기준

- 단순 PK 조회, count, exists는 기본 Repository에 둔다.
- 짧고 고정된 JPQL이면 `@Query`를 우선 검토한다.
- QueryDSL, 커서, 집계, projection, fetch join 전략이 중요하면 custom repository로 올린다.
- QueryDSL은 대체로 custom repository 구현체 안에서 사용한다.

## 1-2) Custom Repository Conventions

- 기본 Repository는 `JpaRepository<Domain, Id>`를 유지한다.
- 복잡 조회는 별도 custom interface로 분리한다.
- 구현 클래스는 `...RepositoryImpl` 네이밍을 따른다.
- 기본 Repository는 custom interface를 함께 상속한다.
- 조회 전용 성격이 더 강하면 `*QueryRepository` 이름도 허용한다.

### 역할 분리 기준

- 기본 Repository
  - 단순 `find`, `count`, `exists`
  - 비교적 짧은 `@Query`
  - 락 조회

- custom repository
  - QueryDSL
  - 복수 조건 조합
  - 집계 + 정렬 + 커서
  - projection 중심 조회
  - fetch join 전략이 중요한 조회

### 반환 타입 기준

- 엔티티가 필요하면 엔티티를 반환한다.
- 집계/경량 조회면 DTO를 반환한다.
- 커서 목록이면 `Slice<T>` 또는 `size + 1` 목록 패턴을 사용한다.
- projection DTO는 repository 전용 DTO 또는 응답 DTO 중 더 적합한 쪽을 사용한다.
- 엔티티 반환과 projection 반환은 이름과 목적이 섞이지 않게 구분한다.

## 2) 구현 위치와 클래스 구조

- QueryDSL 쿼리는 custom repository 구현체에 둔다.
- 인터페이스는 `*RepositoryCustom` 또는 조회 전용 `*QueryRepository`로 분리한다.
- 구현 클래스는 `*RepositoryImpl` 네이밍을 따른다.
- `JPAQueryFactory`는 공통 Config에서 주입받아 사용한다.
- QClass는 메서드 로컬에서 매번 새로 선언하지 말고 재사용 가능한 경우 필드 상수로 둔다.

## 2-1) 현재 프로젝트 스타일

- 구현체는 `RequiredArgsConstructor` + `private final JPAQueryFactory` 구조를 기본으로 둔다.
- 자주 쓰는 QType은 필드로 올리고, 서브쿼리용 별칭 QType만 메서드 안에서 새로 만든다.
- 반환 타입이 DTO면 `Projections.constructor(...)`를 우선 허용한다.
- 정렬 기준이 여러 개면 `orderByConditions(...)` 같은 메서드로 `OrderSpecifier<?>[]`를 반환한다.
- 커서 조건은 `createdDateCursorCondition(...)`, `countCursorCondition(...)`처럼 축별 메서드로 분리한다.
- 정렬 기준별 분기는 `switch`로 모아 두고, 날짜식/집계식도 `sortDateExpression(...)`처럼 메서드로 뺀다.
- optional filter는 `emotionCondition(...)` 같은 null 허용 `BooleanExpression` 메서드로 분리한다.
- 날짜 축 커서는 `createdDate + id`, 집계 축 커서는 `count + id`처럼 tie-breaker까지 한 메서드 안에서 처리한다.
- 최신 대표값 1건은 서브쿼리 `select + orderBy(desc) + limit(1)` 패턴을 허용한다.
- 컬렉션 fetch join이 필요한 조회는 `id` 목록을 먼저 구한 뒤 2차 조회에서 `selectDistinct(entity)` + fetch join + 정렬 복원 패턴을 사용한다.
- Kotlin 구현에서는 `when`, nullable 파라미터, 작은 `private fun` 조건 메서드 분리, `val` 우선, `!!` 회피를 기본으로 둔다.

## 3) 조건식 작성 규칙

- 동적 조건이 여러 개면 `BooleanBuilder`로 모은다.
- 선택 조건 하나를 재사용해야 하면 `BooleanExpression` 메서드로 분리한다.
- 조건 메서드는 null 허용 패턴으로 작성해 `where(...)`, `having(...)`에 바로 넣을 수 있게 한다.
- 분기별로 정렬 기준이나 커서 기준이 달라지면 `switch` 또는 별도 메서드로 분리한다.
- 조건 메서드는 “무슨 조건인지”가 이름에 드러나야 한다.
- 현재 프로젝트에서는 선택 필터는 작은 `BooleanExpression` 메서드로, 범위/복합 조건은 `BooleanBuilder`로 조합하는 방식을 우선한다.

## 4) 정렬과 커서 패턴

- 커서는 정렬 기준과 정확히 같은 축으로 설계한다.
- tie-breaker 없이 단일 정렬 값만 쓰지 않는다.
- 날짜 정렬은 `createdDate + id`처럼 deterministic 하게 만든다.
- 집계 정렬은 `count + id`처럼 보조 정렬 키를 둔다.
- 페이지 크기 판단은 `size + 1` 조회 후 잘라서 `hasNext`를 계산한다.
- 정렬 기준이 바뀌면 커서 조건 메서드도 함께 바뀌어야 한다.
- count 기준 커서는 보통 `having(...)`으로, createdDate 기준 커서는 `where(...)` 또는 정렬 식과 같은 축의 조건 메서드로 맞춘다.
- `SortType`이 있으면 `switch` 하나로 정렬, 커서, 기준 표현식을 같이 관리하는 편을 우선한다.

## 5) 집계와 서브쿼리 패턴

- 집계 projection이 필요하면 DTO 생성자 projection 또는 QDto를 사용한다.
- 그룹핑 기준 컬럼은 `groupBy`에 명시적으로 적는다.
- 최신 1건, 존재 여부, distinct 대표값 선출처럼 파생 메서드로 표현이 애매한 경우 서브쿼리를 허용한다.
- `having`은 그룹 집계 기준 커서나 집계 필터에만 사용하고, 일반 필터는 `where`에 둔다.
- 집계 쿼리는 총합과 항목별 값이 모두 맞는지 테스트로 검증한다.
- 최신 대표값 1건이 필요하면 서브쿼리 `select + orderBy + limit(1)` 패턴을 허용한다.
- 같은 그룹의 최신 값 1개를 뽑아 DTO에 같이 넣는 패턴을 허용한다.

## 6) fetch join과 조회 분리

- to-one 연관을 함께 읽어야 하고 N+1 리스크가 명확하면 fetch join을 사용한다.
- 컬렉션 fetch join + 페이지네이션은 바로 결합하지 않는다.
- 컬렉션이 끼는 경우 2단계 조회를 우선 고려한다.
  - 1차: id 목록만 커서/정렬 기준으로 조회
  - 2차: `where id in (...)` + 필요한 fetch join 수행
  - 마지막에 애플리케이션 레벨에서 원래 순서를 복원
- 엔티티를 반환하는 조회는 연관 접근 패턴까지 생각해서 N+1이 생기지 않게 한다.
- fetch join이 필요 없는 연관까지 습관적으로 묶지 않는다.
- 2단계 조회 후 결과 순서가 깨지면 `orderMap` 같은 인메모리 순서 복원 패턴을 사용한다.
- 현재 프로젝트에서는 컬렉션 포함 상세 조회를 “id 목록 조회 -> fetch join 재조회 -> `Comparator`/`orderMap` 복원” 방식으로 푸는 것을 우선 허용한다.

## 7) N+1 방지 원칙

- 조회 후 루프 안에서 연관 엔티티를 지연 로딩하게 만드는 패턴이 없는지 먼저 확인한다.
- 응답 생성 과정에서 to-one, 컬렉션 연관 접근이 반복되면 N+1 가능성을 의심한다.
- 단건성 연관은 fetch join, 다건 컬렉션은 배치 조회 또는 2단계 조회를 우선 검토한다.
- DTO projection으로 끝낼 수 있는 조회라면 엔티티 그래프를 굳이 로딩하지 않는다.
- N+1 방지는 “일단 fetch join”이 아니라 조회 목적, 페이지네이션, row 중복 가능성까지 같이 보고 선택한다.

## 8) projection 규칙

- DTO 반환 전용 조회는 projection을 허용한다.
- QDto가 이미 있으면 우선 사용하고, 없으면 `Projections.constructor(...)` 또는 팀 표준 projection 방식을 따른다.
- 엔티티 조회가 필요한 메서드에서 무리하게 projection으로 바꾸지 않는다.
- projection은 API/Service가 실제로 쓰는 필드만 조회하도록 최소화한다.
- 현재 프로젝트는 API 응답 DTO를 projection에 직접 쓰는 경우도 허용하지만, 재사용성보다 조회 목적이 먼저 설명 가능해야 한다.

## 9) 테스트 규칙

- QueryDSL Repository 구현은 `@DataJpaTest`와 QueryDSL Config import로 검증한다.
- 성공 케이스만 두지 말고 사용자 필터, 정렬, 집계, 커서, 타 데이터 제외 같은 경계를 함께 테스트한다.
- 집계 쿼리는 총합과 항목별 count를 같이 검증한다.
- 커서 쿼리는 정렬 순서, tie-breaker, 다음 페이지 존재 여부를 검증한다.
- fetch join 또는 2단계 조회를 사용한 경우 기대한 연관이 한 번에 로딩되는지와 결과 순서가 유지되는지 확인한다.
- N+1 리스크가 있는 조회는 최소한 쿼리 구조와 연관 접근 방식이 안전한지 리뷰 포인트로 남긴다.
- 정렬 기준이 여러 개면 각 정렬 타입별 커서 조건과 tie-breaker까지 같이 검증한다.

## 10) 인덱스 기준

- 인덱스는 필터 컬럼과 정렬 키를 함께 보고 설계한다.
- 커서/정렬 조회는 where 절과 order by에 동시에 참여하는 컬럼 조합을 우선 검토한다.
- 중복 보장은 서비스 체크만 두지 말고 unique constraint 필요 여부를 함께 본다.
- 집계/정렬이 잦은 쿼리는 실행 계획과 인덱스 활용 가능성을 같이 확인한다.

## 11) 금지/주의 사항

- 단순 고정 조건 조회까지 QueryDSL로 밀어 넣지 않는다.
- `where`로 처리할 수 있는 일반 필터를 불필요하게 `having`으로 올리지 않는다.
- tie-breaker 없는 커서 정렬을 만들지 않는다.
- fetch join 컬렉션 조회에 바로 offset pagination을 붙이지 않는다.
- 정렬 기준을 바꾸면서 커서 조건 메서드를 같이 바꾸지 않는 상태를 남기지 않는다.
- QType 선언, 조건 메서드, 정렬 메서드를 무분별하게 퍼뜨려 가독성을 해치지 않는다.
- N+1 위험이 있는 엔티티 반환 조회를 검증 없이 추가하지 않는다.
- `SortType` 분기, 기준 표현식, 커서 조건이 서로 따로 놀게 두지 않는다.

## 12) 구현 전에 빠르게 확인할 질문

- 정말 QueryDSL이 필요한 복잡도인가?
- 이 조건은 `where`와 `having` 중 어디에 있어야 맞는가?
- 정렬 기준이 바뀌면 커서 조건과 tie-breaker도 함께 바뀌는가?
- fetch join이 필요한 연관과 불필요한 연관을 구분했는가?
- DTO projection으로 끝낼 수 있는 조회를 불필요하게 엔티티 조회로 만들고 있지 않은가?
- 이 쿼리는 `@DataJpaTest`로 재현 가능한 테스트를 바로 쓸 수 있는가?
- 조회 이후 응답 조립 과정에서 N+1이 생기지 않는가?

## Assets

- `assets/querydsl-style-example.kt`: 현재 프로젝트 스타일 예시

## References

- `references/querydsl-checklist.md`: 도입/성능/테스트 체크리스트
- `references/custom-repository-checklist.md`: custom repository 도입 전 체크리스트

## Completion Gate

- QueryDSL 도입 이유가 명확한가?
- 조건, 정렬, 커서 로직이 읽기 쉬운 메서드로 분리됐는가?
- 집계/서브쿼리/페이지네이션 규칙이 일관적인가?
- fetch join, projection, 2단계 조회 선택이 N+1 없이 설명 가능한가?
- 테스트로 정렬, 커서, 집계, 제외 조건을 검증하는가?
