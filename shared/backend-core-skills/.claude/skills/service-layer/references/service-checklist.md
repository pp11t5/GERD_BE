# Service Checklist

## Responsibility
- 비즈니스 규칙이 Service에 있는가
- Controller가 엔티티 조립이나 상태 변경을 하지 않는가
- DTO는 스키마 역할에만 머무르는가
- 생성자 주입을 사용하고 있는가

## Transaction
- 클래스 기본 `@Transactional(readOnly = true)`가 필요한가
- 쓰기 메서드에만 `@Transactional`이 붙는가
- 외부 호출이 트랜잭션 안에서 오래 머무르지 않는가

## Creation / Conversion
- 엔티티 생성이 Service 또는 Converter 책임인가
- 반복 변환은 converter로 분리됐는가
- 단발성 응답 조립만 Service에 남겼는가

## Kotlin / Nullable
- `!!` 없이 null 안전성이 유지되는가
- nullable 반환이 유즈케이스 의미와 맞는가
- Optional을 그대로 끌고 다니지 않는가

## Coroutine
- 정말 코루틴이 필요한 구간인가
- `suspend`가 책임과 경계를 더 선명하게 만드는가
- `async/await`가 실제 병렬 I/O에만 쓰였는가

## Concurrency
- count + save, 중복 생성, 상태 전이에 락/제약이 필요한가
- `PESSIMISTIC_WRITE` 또는 unique constraint 중 무엇이 맞는가
