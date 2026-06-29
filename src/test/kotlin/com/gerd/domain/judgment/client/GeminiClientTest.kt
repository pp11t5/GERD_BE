package com.gerd.global.ai.gemini

import com.gerd.global.config.properties.GeminiProperties
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.HttpServerErrorException
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress

class GeminiClientTest {

    private lateinit var server: HttpServer
    private lateinit var client: GeminiClient

    // 핸들러가 테스트별 응답을 돌려주도록 가변 상태로 둔다
    private var responseStatus = 200
    private var responseBody = ""
    private var requestCount = 0

    // 운영의 Boot 자동 구성 mapper처럼 Kotlin 모듈을 ServiceLoader로 등록한다
    private val objectMapper = JsonMapper.builder().findAndAddModules().build()

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/") { exchange ->
                requestCount++
                val bytes = responseBody.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(responseStatus, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
        client = GeminiClient(
            geminiProperties = GeminiProperties(
                apiKey = "test-key",
                model = "gemini-test",
                baseUrl = "http://localhost:${server.address.port}",
                connectTimeoutMs = 1000,
                readTimeoutMs = 1000,
            ),
            geminiResponseParser = GeminiResponseParser(),
        )
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    private fun envelope(judgmentJson: String): String =
        objectMapper.writeValueAsString(
            mapOf(
                "candidates" to listOf(
                    mapOf(
                        "content" to mapOf("parts" to listOf(mapOf("text" to judgmentJson))),
                        "finishReason" to "STOP",
                    ),
                ),
            ),
        )

    private fun call() = client.generateJson(GeminiRequest("system", "user", mapOf("type" to "OBJECT")))

    @Nested
    inner class 성공 {

        @Test
        fun `structured output text를 반환한다`() {
            responseBody = envelope(
                """
                {"grade":"CAUTION","personalTitle":"오늘은 천천히 즐겨보세요","reasons":["카페인"],"items":[
                  {"emphasis":"카페인이 들어 있어요","body":"천천히 드세요."},
                  {"emphasis":"알레르기 해당 없어요","body":"포함되지 않았어요."}
                ]}
                """.trimIndent(),
            )

            val text = call()

            assertThat(text).contains("\"grade\":\"CAUTION\"")
            assertThat(text).contains("오늘은 천천히 즐겨보세요")
        }
    }

    @Nested
    inner class 실패 {

        @Test
        fun `HTTP 5xx면 예외를 전파한다(재시도·폴백은 스프링 프록시가 처리한다)`() {
            responseStatus = 503
            responseBody = """{"error":"unavailable"}"""

            assertThrows<HttpServerErrorException> { call() }
        }

        @Test
        fun `HTTP 429(한도 초과)면 null을 반환한다`() {
            responseStatus = 429
            responseBody = """{"error":"rate limit"}"""

            assertThat(call()).isNull()
        }

        @Test
        fun `candidates가 비어 있으면 null을 반환한다`() {
            responseBody = """{"candidates":[]}"""

            assertThat(call()).isNull()
        }

        @Test
        fun `응답 텍스트가 JSON이 아니어도 공통 클라이언트는 그대로 반환한다`() {
            responseBody = envelope("죄송하지만 판단할 수 없습니다")

            assertThat(call()).isEqualTo("죄송하지만 판단할 수 없습니다")
        }

        @Test
        fun `도메인 스키마 검증은 하지 않는다`() {
            responseBody = envelope(
                """{"grade":"CAUTION","reasons":[],"items":[{"emphasis":"하나","body":"뿐"}]}""",
            )

            assertThat(call()).contains("\"grade\":\"CAUTION\"")
        }

        @Test
        fun `API 키가 비어 있으면 HTTP 호출 없이 null을 반환한다`() {
            val blankKeyClient = GeminiClient(
                geminiProperties = GeminiProperties(
                    apiKey = "",
                    model = "gemini-test",
                    baseUrl = "http://localhost:${server.address.port}",
                    connectTimeoutMs = 1000,
                    readTimeoutMs = 1000,
                ),
                geminiResponseParser = GeminiResponseParser(),
            )

            assertThat(blankKeyClient.generateJson(GeminiRequest("system", "user", mapOf("type" to "OBJECT")))).isNull()
            assertThat(requestCount).isZero()
        }
    }
}
