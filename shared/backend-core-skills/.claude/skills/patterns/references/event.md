# Event Patterns

## 발행 위치

- 서비스 트랜잭션 내부에서 `ApplicationEventPublisher`로 이벤트 발행
- DB 상태 변경과 함께 발행해도 실제 처리는 커밋 후로 지연

## 리스너 위치

- `@Component` + `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`
- 외부 I/O (파일 삭제, 캐시 무효화)는 리스너에서 처리
- 후처리가 별도 트랜잭션을 열어야 하면 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 추가

## 안정성 규칙

- 리스너 실패가 메인 요청 성공/실패를 뒤집지 않도록 설계
- 외부 삭제/정리 작업은 실패 시 `log.warn`으로 경고 후 다음 항목 진행
- 루프 내부에서 개별 `try-catch`로 감싸 한 건 실패가 전체를 중단하지 않게 한다

## 사용 기준

- 사이드이펙트(파일 삭제, 캐시 무효화, 알림)를 메인 트랜잭션에서 분리할 때
- 실패해도 메인 흐름에 영향을 주면 안 되는 후처리 작업
- 여러 컴포넌트가 같은 도메인 이벤트에 각자 반응해야 할 때

## 사용하지 않는 경우

- 후처리가 반드시 메인 트랜잭션과 함께 성공/실패해야 하는 경우 → 같은 트랜잭션에서 직접 처리
- 즉각적인 응답 데이터를 만들기 위한 용도

## 구현 패턴

### 이벤트 클래스

```kotlin
data class [Domain]DeletedEvent(
    val [domain]Id: Long,
    val imageKeys: List<String>,
)
```

### 서비스에서 발행

```kotlin
@Transactional
fun delete[Domain]([domain]Id: Long, userId: Long) {
    // ...상태 변경...
    eventPublisher.publishEvent([Domain]DeletedEvent([domain]Id, imageKeys))
}
```

### 리스너

```kotlin
private val log = LoggerFactory.getLogger([Domain]DeletedEventListener::class.java)

@Component
class [Domain]DeletedEventListener(
    private val r2Service: R2Service,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(event: [Domain]DeletedEvent) {
        for (imageKey in event.imageKeys) {
            try {
                r2Service.deleteFile(imageKey)
            } catch (e: Exception) {
                log.warn("[[DOMAIN]] R2 스토리지 이미지 삭제 실패 [domain]Id={}, key={}", event.[domain]Id, imageKey, e)
            }
        }
    }
}
```

## 네이밍 규칙

- 이벤트: `[Domain][행위]Event` — `PostDeletedEvent`, `UserProfileUpdatedEvent`
- 리스너: `[Domain][행위]EventListener` — `PostDeletedEventListener`
