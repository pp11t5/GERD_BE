# Coroutine Strategy

## Purpose

이 스킬은 현재 프로젝트에서 Kotlin Coroutine 도입 여부를 판단할 때 사용한다.
기본 전제는 이 프로젝트가 **Spring MVC + Spring Data JPA** 기반이라는 점이다.

Coroutine은 기본 패턴이 아니다. 도입 이점이 명확할 때만 제한적으로 사용한다.

## Critical Rules

- 현재 프로젝트의 기본 서비스 구현은 동기 방식으로 유지한다.
- JPA 트랜잭션 서비스 계층에 `suspend`를 기본 적용하지 않는다.
- DB 접근이 중심인 CRUD/도메인 로직에는 Coroutine을 도입하지 않는다.
- Coroutine은 외부 I/O 병렬화, 장시간 대기, 스트리밍, 비동기 API 조합이 핵심일 때만 검토한다.
- blocking JPA 위에 형식적으로 `suspend`만 덧씌우는 방식은 금지한다.

## 도입을 검토하는 경우

- 외부 API 여러 개를 병렬 호출해야 할 때
- 파일 업로드/다운로드, 외부 스토리지 접근처럼 I/O 대기 시간이 길 때
- SSE, streaming response처럼 응답 스트리밍이 필요할 때
- callback, `CompletableFuture`보다 Coroutine이 더 단순한 orchestration을 만들 때

## 도입하지 않는 경우

- 단순 CRUD API
- JPA Repository 호출이 대부분인 서비스
- "Kotlin이니까 Coroutine도 쓰자" 수준의 막연한 이유
- 동기 코드보다 복잡도만 증가하는 경우

## 허용 영역 / 비권장 영역

**허용**
- 외부 API 호출 전용 client 계층
- 여러 비동기 결과를 조합하는 read-only facade
- 스트리밍 응답 처리 계층

**비권장**
- JPA Repository
- QueryDSL Repository
- 트랜잭션 상태 변경 서비스
- 도메인 핵심 로직 전체를 suspend 기반으로 바꾸는 것

## JPA와 함께 사용할 때 주의

- JPA 호출은 본질적으로 blocking — `suspend fun` 안에서 호출해도 non-blocking이 되지 않는다.
- 트랜잭션이 걸린 엔티티를 suspend 경계 밖으로 오래 끌고 나가지 않는다.
- lazy loading 엔티티를 비동기 흐름 곳곳에 전달하지 않는다.
- JPA 결과는 필요한 시점에 DTO로 변환해 넘긴다.

## 구현 전에 확인할 질문

- 이 작업은 진짜 I/O 병렬화 이점이 있는가?
- JPA 트랜잭션 로직과 분리할 수 있는가?
- timeout/cancel/retry 정책이 있는가?
- 동기 코드보다 실제로 더 단순해지는가?

하나라도 애매하면 먼저 동기 방식으로 구현한다.
