---
name: jpa-entity
description: 현재 프로젝트의 엔티티 생성/상태 변경 규칙을 정리한 스킬. 엔티티 생성 위치, Service와 Converter 책임 분리, 정적 팩터리와 builder 사용 기준을 포함한다.
---

# JPA Entity

## Purpose

이 스킬은 현재 프로젝트의 엔티티 생성/상태 변경 패턴에 맞춰 엔티티를 설계하거나 수정할 때 사용한다.
일반적인 JPA 원칙보다 실제 팀 규칙인 생성 위치, builder 허용 범위, Service와 Converter 책임 분리를 우선한다.

## Critical Rules

- 엔티티 생성은 Controller나 DTO가 아니라 Service 또는 Converter에서 수행한다.
- DTO 내부에 엔티티 생성 로직을 넣지 않는다.
- 엔티티 생성 시점에 필요한 연관 엔티티 조회와 권한 검증은 Service가 먼저 수행한다.
- 저장 전 기본값, 파생 필드, 연관관계는 엔티티 생성자나 도메인 메서드에서 맞춘다.
- 생성 후 상태 변경은 setter 대신 의미 있는 도메인 메서드로 수행한다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- JPA용 no-arg 생성자는 `kotlin("plugin.jpa")`가 바이트코드 레벨에서 자동 생성하므로 코드에 직접 작성하지 않는다.
- 정적 팩터리 메서드(`companion object create()`, `of()`)는 **단순 생성에는 추가하지 않는다**. 생성 시 강제할 규칙(파생값, 기본 상태, 선검증, 생성 경로 제한)이 있을 때만 둔다.
- 양방향 매핑은 기본값이 아니다. 반대 방향 탐색이 실제 조회/검색/집계 쿼리에서 필요할 때만 추가한다.
- Kotlin 엔티티는 Java 스타일 builder를 기본 패턴으로 삼지 않는다. 기본은 primary constructor + named parameter다.
- 연관관계 fetch 기본값은 JPA spec이 아니라 팀 규칙으로 맞춘다. `@ManyToOne`, `@OneToOne`은 항상 `fetch = FetchType.LAZY`를 명시한다.
- 필수 연관관계는 `optional = false` + `@JoinColumn(nullable = false)` + Kotlin non-null 타입을 함께 맞춘다.
- 선택 연관관계만 `optional = true`와 nullable 타입을 허용한다. nullable인데 `nullable = false` 같은 불일치는 금지한다.
- cascade는 aggregate 경계 안에서 생명주기를 함께 관리할 때만 제한적으로 사용한다. 기본값은 cascade 없음이다.
- `orphanRemoval = true`는 부모가 자식 생명주기를 독점할 때만 사용한다. 조회 편의용 연관관계에는 쓰지 않는다.
- `@Table(name = ...)` 값은 특별한 예외가 없으면 복수형으로 지정한다. 예: `users`, `posts`, `order_items`

## Kotlin JPA 생성자 패턴

```kotlin
// 단순 엔티티 — 그냥 primary constructor 사용
@Entity
class Post(
    val title: String,
    val content: String,
    @ManyToOne(fetch = FetchType.LAZY)
    val author: User,
) : BaseEntity()
```

```kotlin
// 생성 시 강제할 규칙이 있을 때만 companion object 추가
// (파생 필드 계산, 선검증, 기본 상태, 생성 경로 제한 등)
@Entity
class Post(
    val title: String,
    val slug: String,
    @ManyToOne(fetch = FetchType.LAZY)
    val author: User,
) : BaseEntity() {

    companion object {
        fun create(title: String, author: User): Post =
            Post(title = title, slug = title.toSlug(), author = author)
    }
}
```

- no-arg 생성자: `kotlin-jpa` 플러그인이 바이트코드에 자동 삽입 — 명시적 작성 불필요
- 단순 위임(`fun of(...) = Entity(...)`)은 보일러플레이트 — 추가하지 않는다
- 생성 시 강제할 규칙이 있을 때만 `companion object` 도입

## 1) 생성 방식 기준

- 정적 팩터리 메서드 사용
  - 파생 필드 계산, 선검증, 기본 상태 주입, 여러 명명된 생성 경로처럼 생성 시 강제할 규칙이 있을 때만 사용한다.
  - 단순히 생성자 호출을 감싸는 `fun of(...) = Entity(...)` 패턴은 추가하지 않는다.
  - Kotlin named parameter로 생성 의도가 충분히 드러나면 그걸로 대체한다.

- builder 사용
  - Kotlin에서는 기본 선택지가 아니다. primary constructor + named parameter로 충분하면 builder를 만들지 않는다.
  - 생성 파라미터가 매우 많고 optional 값 조합이 복잡할 때만 예외적으로 허용한다.
  - 테스트 fixture에서만 가독성 이점이 큰 경우 제한적으로 허용한다.
  - builder 내부 `build()`에서 `requireNotNull`, `trim`, `require` 같은 선검증을 수행한다.

## 2) Service와 Converter 책임 분리

- Service에서 생성하는 경우
  - 생성 전에 권한 확인, 존재 여부 확인, 중복 검증, 락 획득 같은 선행 규칙이 필요한 경우
  - 연관 엔티티 조회 결과를 조합해야 하는 경우
  - sourceType, createdBy, 상태 초기값 같은 비즈니스 규칙을 함께 넣어야 하는 경우

- Converter에서 생성하는 경우
  - 외부 응답 DTO를 내부 엔티티로 순수 매핑할 때 허용한다.
  - 비즈니스 검증, 저장 책임, 권한 확인은 Converter에 넣지 않는다.
  - 같은 외부 DTO 매핑이 반복되면 Converter로 분리한다.

## 3) 엔티티 내부에서 처리하는 것

- 생성 시 항상 함께 맞춰야 하는 필드 초기화
- 입력값으로부터 파생되는 필드 계산
- 외부에서 임의로 바꾸면 안 되는 초기 일관성
- 생성 후 여러 필드가 함께 움직이는 상태 전이
- 템플릿 스타일처럼 `createdAt`, `updatedAt`, 파생 시간 필드 등은 생성 경로에서 함께 맞춘다.

## 4) 상태 변경 규칙

- 상태 변경은 필드별 setter 대신 의미 있는 메서드로 노출한다.
- 변경 메서드는 한 도메인 행위를 나타내는 이름을 사용한다.
- 여러 필드가 함께 움직이면 하나의 메서드 안에서 묶어 변경한다.

## 5) 연관관계 생성 규칙

- 연관 엔티티는 ID만 세팅한 가짜 객체로 만들지 말고 Repository 조회 결과를 사용한다.
- 생성 전에 소유권과 존재 여부를 검증한다.
- 단방향 매핑을 우선한다. `@ManyToOne`, `@OneToOne` 소유 측만으로 충분하면 반대편 컬렉션/참조를 만들지 않는다.
- 양방향 매핑은 검색, 목록 조회, fetch join, 집계, 도메인 탐색이 반복되어 반대 방향 접근이 실제로 필요한 경우에만 추가한다.
- 양방향 매핑을 추가할 때만 연관 컬렉션을 엔티티 필드에서 기본 초기화한다.
- 양방향 컬렉션을 두는 경우에만 편의 메서드로 양쪽을 동기화한다. 단순히 "있으면 좋을 것 같아서" 추가하지 않는다.
- `@ManyToOne`, `@OneToOne`은 항상 `fetch = FetchType.LAZY`를 명시한다. 기본 eager에 기대지 않는다.
- 필수 연관은 `optional = false`, 선택 연관은 `optional = true`로 의도를 드러낸다.
- DB null 제약과 Kotlin 타입을 함께 맞춘다. 필수 연관은 non-null + `nullable = false`, 선택 연관은 nullable + `nullable = true` 또는 기본값 생략으로 맞춘다.
- 테이블명은 특별한 예외가 없으면 복수형으로 통일한다.
- `@OneToMany`는 읽기 전용 탐색/조회 편의를 위해 습관적으로 열지 않는다.
- cascade는 `PERSIST`, `MERGE` 등 필요한 범위만 좁게 선택한다. `CascadeType.ALL`은 aggregate 생명주기가 명확할 때만 허용한다.
- `orphanRemoval = true`는 부모 컬렉션에서 제거되면 자식이 삭제되어야 하는 진짜 포함 관계에서만 사용한다.

## 6) Kotlin 엔티티 필드 선언 기준

- **`data class` 사용 금지**: JPA 지연 로딩은 프록시 객체를 생성하는데, `data class`의 자동 생성 `equals()`/`hashCode()`가 프록시 타입 불일치를 일으킬 수 있다. 엔티티는 항상 일반 `class`를 사용한다.
- **`val` 우선**: 변경이 필요 없는 필드(id, 생성일, 불변 속성)는 `val`로 선언한다.
- **`var` 허용 범위**: 비즈니스 상태 변경이 필요한 필드만 `var`로 선언한다.
- **nullable 기준**: DB 컬럼이 NOT NULL이면 Kotlin Non-null 타입(`String`), NULL 허용이면 nullable 타입(`String?`)으로 선언한다. 컴파일 시점에 NullPointerException을 방지한다.
- **기본값 남용 금지**: JPA 편의를 위해 의미 없는 기본값(`""`, `0L`)을 남발하지 않는다. 실제 도메인 기본값만 constructor default로 둔다.
- **지연 로딩 연관관계는 nullable 여부를 도메인 제약에 맞춘다**: 필수 연관은 non-null, 선택 연관은 nullable로 선언한다.

```kotlin
@Entity
class Post(
    val title: String,          // NOT NULL → Non-null
    var content: String,        // 수정 가능 → var
    val authorId: Long,         // FK NOT NULL → Non-null val
    var deletedAt: LocalDateTime? = null,  // nullable 컬럼
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseEntity()
```

## 7) 금지/주의 사항

- DTO에 `toEntity()`를 추가하지 않는다.
- Controller에서 엔티티를 직접 조립하지 않는다.
- setter를 열어 두고 서비스가 필드를 여기저기 직접 바꾸게 하지 않는다.
- 연관 엔티티 조회 없이 외래키 값만 믿고 엔티티를 만들지 않는다.
- builder로 생성하면서 Service가 반드시 보장해야 할 검증을 생략하지 않는다.
- 생성과 수정 규칙이 섞인 범용 `saveOrUpdate` 스타일 메서드를 엔티티에 두지 않는다.
- `data class`로 엔티티를 선언하지 않는다.
- 조회 요구가 불명확한데도 습관적으로 양방향 매핑과 `MutableList` 컬렉션을 먼저 추가하지 않는다.
- Kotlin 엔티티를 Java Bean처럼 무의미한 setter 집합으로 만들지 않는다.

## 8) 구현 전에 빠르게 확인할 질문

- 이 엔티티 생성은 Service가 해야 하는가, Converter가 해야 하는가?
- 정적 팩터리 이름이 생성 의도를 더 잘 드러내는가?
- 생성 시 같이 맞춰야 하는 파생 필드가 있는가?
- 생성 후 수정은 setter가 아니라 도메인 메서드로 표현되는가?
- 연관 엔티티 존재/권한 검증이 생성 전에 끝났는가?
- 반대 방향 탐색이 정말 필요한가, 아니면 단방향 + QueryDSL 쿼리로 충분한가?

## Assets

- `assets/entity-style-example.kt`: 현재 프로젝트 스타일 엔티티 예시
- `assets/entity-template.kt`: 새 엔티티 작성 시 복사해서 시작하는 기본 템플릿

## References

- `references/entity-creation-checklist.md`: 엔티티 생성 전/후 체크리스트

## Completion Gate

- 생성 경로가 한눈에 보이는가?
- 상태 변경이 메서드로 캡슐화됐는가?
- 생성 책임이 DTO가 아니라 Service 또는 Converter에 있는가?
- 연관관계 조회와 권한 검증이 생성 전에 보장되는가?
