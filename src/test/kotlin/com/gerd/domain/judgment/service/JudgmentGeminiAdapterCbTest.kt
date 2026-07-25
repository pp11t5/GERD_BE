package com.gerd.domain.judgment.service

import com.gerd.global.ai.LlmTimeoutException
import com.gerd.global.ai.gemini.GeminiClient
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException

/**
 * @CircuitBreaker AOP 프록시가 활성화된 컨텍스트에서 CB 상태 전이를 검증
 *
 * 테스트 프로파일 CB 설정: sliding-window-size=3, failure-rate-threshold=100
 * → 3회 연속 실패 시 OPEN
 */
@SpringBootTest
@ActiveProfiles("test")
class JudgmentGeminiAdapterCbTest {

    @Autowired
    private lateinit var adapter: JudgmentGeminiAdapter

    @MockitoBean
    private lateinit var geminiClient: GeminiClient

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    private lateinit var cb: CircuitBreaker

    @BeforeEach
    fun reset() {
        cb = circuitBreakerRegistry.circuitBreaker("gemini-judgment")
        cb.reset()
    }

    private fun call() = adapter.generateJudgment("system", "user", mapOf("type" to "OBJECT"))

    @Nested
    inner class `정상 흐름` {

        @Test
        fun `성공 시 CB는 CLOSED 상태를 유지한다`() {
            whenever(geminiClient.generateJson(any())).thenReturn(null)

            call()

            assertThat(cb.state).isEqualTo(CircuitBreaker.State.CLOSED)
        }
    }

    @Nested
    inner class `CB OPEN` {

        @Test
        fun `슬라이딩 윈도우 내 실패가 임계값 초과하면 OPEN으로 전이된다`() {
            whenever(geminiClient.generateJson(any()))
                .thenThrow(timeout())

            repeat(3) { call() }

            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)
        }

        @Test
        fun `CB OPEN 상태에서 Gemini를 호출하지 않고 즉시 폴백을 반환한다`() {
            whenever(geminiClient.generateJson(any()))
                .thenThrow(timeout())
            repeat(3) { call() } // CB OPEN

            val result = call()

            assertThat(result).isNull()
            verify(geminiClient, times(3)).generateJson(any()) // 4번째 호출 없음
        }
    }

    private fun timeout() = LlmTimeoutException(
        ResourceAccessException("timeout", SocketTimeoutException("read timeout")),
    )
}
