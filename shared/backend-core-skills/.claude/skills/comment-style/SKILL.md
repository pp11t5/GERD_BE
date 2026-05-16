---
name: comment-style
description: Kotlin Spring 백엔드 주석 작성 기준. 클래스/메서드/로직 주석을 "왜" 중심으로 일관되게 작성한다. 주석 추가/개선 요청 시 이 스킬을 사용한다.
---

# Comment Style

## Purpose

코드로 설명할 수 없는 "왜"를 주석으로 남긴다.
구현 의도, 정책 선택, 비즈니스 제약처럼 코드만 봐서는 알 수 없는 맥락을 대상으로 한다.

## Critical Rules

- 코드로 설명 가능한 것은 주석을 달지 않는다
- "무엇"이 아니라 "왜"를 설명한다
- 한 줄 주석을 기본으로 사용한다
- 주석 문장 끝에 마침표를 붙이지 않는다
- 기존 코드의 주석 톤과 길이를 맞춘다
- TODO는 실행 조건이나 후속 맥락을 함께 남긴다
- 죽은 코드(주석 처리된 코드)는 커밋하지 않는다

## 1. 클래스 주석 (KDoc)

클래스의 역할과 책임만 설명한다. 구현 상세는 넣지 않는다.
핵심 기능을 2~4개 bullet로 요약한다.

```kotlin
/**
 * [도메인] [역할]
 *
 * - 기능 1
 * - 기능 2
 */
```

**적용 기준**

| 계층 | 기준 |
|---|---|
| Service / Controller | 필수 |
| Repository | 커스텀 쿼리가 복잡할 때만 |
| Entity | 도메인 제약이 있을 때만 |
| Config | 설정 목적이 이름으로 명확하면 생략 |

**예시**

```kotlin
/**
 * 사용자 인증을 담당하는 서비스
 *
 * - JWT 토큰 발급 및 검증
 * - 로그인/로그아웃 처리
 * - 토큰 재발급
 */
@Service
class AuthService(...)
```

## 2. 메서드 주석 (KDoc)

public API 또는 의도가 불명확한 메서드에만 달고, 이름으로 충분히 설명되면 생략한다.

```kotlin
/**
 * [무엇을 하는 메서드인지 한 줄 설명]
 *
 * @param paramName 설명
 * @return 반환값 설명
 * @throws ExceptionType 발생 조건 (선택)
 */
```

**예시**

```kotlin
/**
 * 사용자 식별자로 활성 상태인 사용자를 조회한다
 *
 * @param userId 조회할 사용자 ID
 * @return 사용자 엔티티
 * @throws GeneralException USER_NOT_FOUND — 탈퇴하거나 존재하지 않는 경우
 */
fun getActiveUser(userId: Long): User
```

## 3. 로직 주석 (인라인)

복잡한 흐름, 성능 최적화, 정책 선택에만 사용한다.

**단일 라인**

```kotlin
// [이 방식을 선택한 이유]
```

**블록 (드물게)**

```kotlin
/*
 * [배경]
 * [선택 이유]
 */
```

**권장 패턴**

```kotlin
// N+1 방지를 위해 fetch join으로 연관 엔티티를 한 번에 조회
// soft delete 정책 — deletedAt이 null인 데이터만 유효한 상태로 간주
// AFTER_COMMIT에서 실행해 DB 커밋 이후 외부 알림 전송 보장
// 동시 생성 방지를 위해 PESSIMISTIC_WRITE 락 적용
// Redis 캐시를 먼저 조회해 DB 부하 감소
```

**TODO 작성 기준**

```kotlin
// TODO: User 엔티티 도입 후 CurrentUserArgumentResolver 등록
// TODO: 트래픽 증가 시 count 쿼리를 집계 테이블로 전환 검토
```


## 4. Workflow

1. 주석이 없어도 코드가 읽히는지 먼저 판단한다
2. 이름/구조 개선으로 주석이 불필요해지면 주석 대신 코드를 개선한다
3. 그래도 의도 전달이 어려운 곳에만 주석을 추가한다
4. 기존 주석 톤과 길이를 맞춰 작성한다

## Completion Gate

- 주석이 "왜"를 설명하는가?
- 코드로 설명 가능한 내용에 주석을 달지 않았는가?
- 문장 끝에 마침표가 없는가?
- 죽은 코드가 주석으로 남아 있지 않은가?
- TODO에 후속 맥락이 있는가?

## References

- `references/forbidden-patterns.md`: 금지 패턴 목록과 판단 기준
