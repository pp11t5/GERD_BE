# Test Structure Template

```kotlin
@Nested
inner class 성공 {

    @Test
    fun `[도메인]을 정상적으로 조회한다`() {
        // given

        // when

        // then
    }
}

@Nested
inner class 실패 {

    @Test
    fun `권한이 없으면 NOT_AUTHORIZED를 반환한다`() {
        // given

        // when

        // then
    }
}
```

## Assertions
- status
- `isSuccess`
- `code`
- `result`
- `ErrorCode`
