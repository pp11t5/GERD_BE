package com.gerd.global.ai.gemini

import com.gerd.global.config.RetryConfig
import com.gerd.global.config.properties.GeminiProperties
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

/**
 * Spring 프록시가 활성화된 컨텍스트에서 @Retryable 동작을 검증한다.
 * GeminiClientTest(단위)는 직접 인스턴스화라 프록시가 없어 5xx 재시도를 검증할 수 없다.
 */
@SpringJUnitConfig(classes = [GeminiClientRetryIT.Cfg::class])
class GeminiClientRetryIT {

    @TestConfiguration
    @Import(GeminiClient::class, GeminiResponseParser::class, RetryConfig::class)
    class Cfg {

        @Bean
        fun objectMapper() = JsonMapper.builder().findAndAddModules().build()

        @Bean
        fun geminiProperties() = GeminiProperties(
            apiKey = "test-key",
            model = "test-model",
            baseUrl = "http://localhost:${mockPort}",
            connectTimeoutMs = 500,
            readTimeoutMs = 500,
            retryDelayMs = 10,
        )
    }

    companion object {
        private val responseQueue = ArrayDeque<Pair<Int, String>>()
        val requestCount = AtomicInteger(0)
        val mockPort: Int

        init {
            val server = HttpServer.create(InetSocketAddress(0), 0).apply {
                createContext("/") { ex ->
                    requestCount.incrementAndGet()
                    val (status, body) = responseQueue.removeFirstOrNull() ?: (200 to "{}")
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    ex.responseHeaders.add("Content-Type", "application/json")
                    ex.sendResponseHeaders(status, bytes.size.toLong())
                    ex.responseBody.use { it.write(bytes) }
                }
                start()
            }
            mockPort = server.address.port
        }

        private fun goodBody(grade: String = "RECOMMEND") =
            """{"candidates":[{"content":{"parts":[{"text":"{\"grade\":\"$grade\",\"personalTitle\":\"좋아요\",\"reasons\":[],\"items\":[{\"emphasis\":\"a\",\"body\":\"b\"},{\"emphasis\":\"c\",\"body\":\"d\"}]}"}]},"finishReason":"STOP"}]}"""

        private val errorBody = """{"error":{"code":503,"message":"Service Unavailable"}}"""
    }

    @Autowired
    private lateinit var geminiClient: GeminiClient

    @BeforeEach
    fun reset() {
        responseQueue.clear()
        requestCount.set(0)
    }

    private fun call() = geminiClient.generateJson(GeminiRequest("system", "user", mapOf("type" to "OBJECT")))

    @Test
    fun `503 두 번 후 성공하면 결과를 반환하고 총 3회 호출된다`() {
        responseQueue += 503 to errorBody
        responseQueue += 503 to errorBody
        responseQueue += 200 to goodBody()

        val result = call()

        assertThat(result).isNotNull()
        assertThat(requestCount.get()).isEqualTo(3)
    }

    @Test
    fun `503이 maxAttempts만큼 반복되면 null을 반환하고 총 3회 호출된다`() {
        repeat(3) { responseQueue += 503 to errorBody }

        val result = call()

        assertThat(result).isNull()
        assertThat(requestCount.get()).isEqualTo(3)
    }

    @Test
    fun `첫 번째 시도에서 성공하면 단 한 번만 호출된다`() {
        responseQueue += 200 to goodBody()

        val result = call()

        assertThat(result).isNotNull()
        assertThat(requestCount.get()).isEqualTo(1)
    }

    @Test
    fun `429(한도 초과)는 재시도 없이 null을 반환하고 1회만 호출된다`() {
        responseQueue += 429 to """{"error":{"code":429,"message":"Too Many Requests"}}"""

        val result = call()

        assertThat(result).isNull()
        assertThat(requestCount.get()).isEqualTo(1)
    }
}
