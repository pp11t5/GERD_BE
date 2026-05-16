---
name: code-review
description: 위험도 중심으로 백엔드 변경을 검토하는 스킬. 버그, 회귀, 보안, 성능, 테스트 누락, 운영 리스크를 근거 중심으로 식별한다.
---

# Code Review

## Purpose

이 스킬은 PR/변경사항 리뷰에서 요약보다 결함 탐지를 우선할 때 사용한다.
현재 프로젝트의 계층 규칙, 템플릿 스타일, 보안/성능/트랜잭션 기준을 어기는 변경을 우선 찾는다.

## Critical Rules

- 파일/라인/행동 기준으로 설명한다.
- Findings를 심각도 순으로 정리한다.
- 테스트 누락은 별도 리스크로 명시한다.
- 재현 가능성, 회귀 가능성, 운영 영향이 큰 문제를 우선한다.
- 템플릿 스타일과 팀 규칙을 어겼더라도 실제 버그/리스크가 아닌 단순 취향 지적은 뒤로 미룬다.
- 수정 제안은 가능하면 현재 저장소 패턴에 맞춘다.

## Review Passes

1. API 계약/Validation/에러 응답
2. Controller 책임 침범 여부
3. Service 책임/트랜잭션 경계/동시성
4. Entity 생성/상태 변경 규칙 위반 여부
5. Repository/Query/N+1/락/인덱스
6. 보안/권한/민감정보
7. 운영성/로그/메트릭/장애 전파
8. 테스트 공백과 회귀 위험

## Detailed Checklist

- API/Controller
  - `ApiResponse`, 상태코드, `ErrorCode`가 계약과 일치하는가
  - Controller가 비즈니스 로직이나 엔티티 조립을 하고 있지 않은가
  - Swagger 설명과 실제 동작이 어긋나지 않는가

- Service
  - 클래스 기본 `@Transactional(readOnly = true)`와 쓰기 메서드 `@Transactional` 원칙이 지켜지는가
  - 외부 호출이 장시간 트랜잭션 안에 들어가 있지 않은가
  - 서비스가 HTTP 타입, Swagger, 응답 포맷 생성 책임을 침범하지 않는가
  - `count + save`, 중복 생성, 상태 전이에서 동시성 리스크가 없는가
  - 생성자 주입 대신 필드 주입이나 테스트하기 어려운 구조를 남기지 않았는가
  - nullable 처리와 예외 전환이 서비스 경계에서 일관적인가
  - 코루틴 도입이 실제 이점 없이 복잡도만 늘리지 않았는가

- Entity/Converter
  - DTO에 `toEntity()` 같은 엔티티 생성 로직이 들어가지 않았는가
  - 엔티티 생성이 Service 또는 Converter 책임으로 유지되는가
  - setter 대신 의미 있는 도메인 메서드로 상태 변경하는가
  - 연관 엔티티를 조회 없이 가짜 객체로 연결하지 않는가

- Repository/QueryDSL
  - 단순 조회를 QueryDSL로 과설계하지 않았는가
  - `where`와 `having` 위치가 맞는가
  - 커서 정렬에 tie-breaker가 있는가
  - fetch join, projection, 2단계 조회 선택이 N+1 없이 설명 가능한가
  - 인덱스와 unique constraint 필요 여부를 함께 검토했는가

- Kotlin/코드 스타일
  - `!!` 사용 없이 null 안전성이 유지되는가
  - Java `Optional`, 불필요한 getter/setter, 장황한 보일러플레이트를 그대로 옮기지 않았는가
  - 불필요한 스코프 함수, 한 번만 쓰는 1~2줄 래퍼 private 메서드가 없는가
  - 템플릿 생성 코드를 도메인 규칙 없이 그대로 커밋하지 않았는가
  - 이름이 도메인 행위와 책임을 설명하는가
  - 코프링 스타일에 맞게 생성자 주입, `val` 우선, data class 활용이 자연스러운가

- Coroutine / Async
  - `suspend`가 붙은 메서드가 실제 비동기 I/O 경계를 반영하는가
  - `async/await`가 병렬 I/O가 아닌 단순 순차 흐름에 남용되지 않았는가
  - 블로킹 JPA 호출과 코루틴 사용이 어색하게 섞여 있지 않은가
  - 예외 처리와 타임아웃이 비동기 흐름에서도 읽히는가

- Security/Operations
  - 인증만 있고 인가가 빠진 부분은 없는가
  - 민감정보가 로그/예외/응답에 노출되지 않는가
  - fallback, 타임아웃, 장애 전파 경로가 설명 가능한가

- Tests
  - 성공/실패, 권한, 존재 오류, 상태 충돌 분기가 빠지지 않았는가
  - Query/집계/커서/N+1 위험 구간은 재현 가능한 테스트가 있는가
  - 버그 수정이면 회귀 테스트가 추가됐는가

## Workflow

1. 변경 의도와 영향 범위를 먼저 고정한다.
2. 위 Review Passes 순서로 리스크를 스캔한다.
3. 버그/회귀/운영 장애 가능성이 큰 항목부터 근거를 모은다.
4. 테스트 공백과 재현 방법을 함께 정리한다.
5. Findings, Open Questions, Residual Risks 순으로 보고한다.

## Kofring Review Focus

- Java 스타일을 Kotlin 문법으로만 옮긴 코드인지 본다.
- 생성자 주입, null 안전성, 불변성, data class 사용이 자연스러운지 본다.
- 코루틴을 쓴다면 “왜 필요한지”와 “어디서 이점이 있는지”가 코드에서 드러나는지 본다.

## Assets

- `assets/review-template.md`: 리뷰 결과 정리 템플릿

## Output Format

```markdown
## Findings
- [Severity] file:line - 문제와 영향

## Open Questions
- 확인이 필요한 가정

## Residual Risks
- 테스트 공백
- 운영 리스크
```

## Severity Guide

- Critical: 보안 취약점, 데이터 손상, 즉시 장애 가능성
- High: 주요 기능 회귀, 잘못된 권한 처리, 동시성 버그, N+1 대량 발생
- Medium: 특정 조건에서 계약 불일치, 테스트 공백, 유지보수성 저하
- Low: 즉시 장애는 아니지만 장기적으로 규칙을 깨는 구조적 문제

## When Not To Use

- 단순 스타일 맞춤이나 자동 포맷 변경만 있는 경우
