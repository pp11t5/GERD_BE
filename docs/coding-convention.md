# 코딩 컨벤션

## 1. 문서 목적
이 문서는 백엔드 팀의 코드 작성 기준을 정리한다.  
세부 구현 규칙은 각 스킬 파일을 참조한다.

## 2. 언어 및 환경

- **Language**: Kotlin
- **JVM**: Java 21
- **Framework**: Spring Boot 4 (Spring Framework 6)
- **Build**: Gradle (Kotlin DSL)

## 3. 패키지 / 계층 구조

```
src/main/kotlin/com/<project>/
├── global/           # 공통 응답, 예외, 설정, 유틸
└── domain/
    └── <domain>/
        ├── controller/
        ├── service/
        ├── repository/
        ├── entity/
        ├── dto/
        └── security/   # 도메인 전용 보안 컴포넌트 (예: auth/security)
```

- `global/security`는 제거, 보안 컴포넌트는 해당 도메인(`domain/auth/security`) 하위에 위치

## 4. 계층별 책임

| 계층 | 책임 |
|---|---|
| Controller | 입출력 처리, 권한 확인, ApiResponse 래핑 |
| Service | 비즈니스 로직, 트랜잭션 경계 |
| Repository | 데이터 접근, QueryDSL 조회 |
| Entity | 도메인 상태와 불변식 |
| DTO | 요청/응답 스키마 |

상세 규칙 → [spring-api skill](../.claude/skills/spring-api/SKILL.md)

- DTO 클래스명은 `DTO`로 끝낸다

## 5. 엔티티 규칙

- 엔티티 생성은 Service 또는 Converter에서 수행
- DTO에 `toEntity()` 금지
- 상태 변경은 의미 있는 도메인 메서드로만 노출
- JPA용 기본 생성자는 `protected`

**기본키 전략**

| 용도 | 타입 | 전략 |
|---|---|---|
| 내부 PK (JOIN, FK) | `Long` | `@GeneratedValue(IDENTITY)` |
| 외부 노출 ID (API 응답) | `UUID` | `@UuidGenerator(style = TIME)` — `BaseEntity.externalId` |

- 순차 Long으로 조인 성능을 확보하고, 외부에는 UUID를 노출해 순차 열거 공격을 방지
- `externalId`는 `BINARY(16)` 컬럼, `updatable = false`로 불변 보장
- API 응답에서 엔티티 id(`Long`) 직접 노출 금지 — `externalId` 사용

상세 규칙 → [jpa-entity skill](../.claude/skills/jpa-entity/SKILL.md)

## 6. Service 계층 규칙

- Command / Query 책임 분리 (CQRS)
- `@Transactional` 경계는 Service 메서드 단위
- 읽기 전용 조회는 `@Transactional(readOnly = true)`

상세 규칙 → [service-layer skill](../.claude/skills/service-layer/SKILL.md)

## 7. QueryDSL 규칙

- 동적 조건 조회는 Custom Repository로 분리
- Projection DTO는 `repository/dto` 패키지에 위치
- Projection DTO를 포함한 모든 DTO 클래스명은 `DTO`로 끝낸다
- API 응답 DTO와 Repository DTO를 혼용하지 않음

상세 규칙 → [querydsl skill](../.claude/skills/querydsl/SKILL.md)

## 8. 예외 처리

- 예외는 `CustomException` + 도메인 `*ErrorCode` 기준
- Controller에서 직접 삼키지 않고 전역 핸들러로 위임
- 내부 메시지/스택은 외부에 노출하지 않음
