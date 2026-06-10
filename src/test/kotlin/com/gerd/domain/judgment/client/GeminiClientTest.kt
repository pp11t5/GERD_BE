package com.gerd.domain.judgment.client

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.global.config.properties.GeminiProperties
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress

class GeminiClientTest {

    private lateinit var server: HttpServer
    private lateinit var client: GeminiClient

    // 핸들러가 테스트별 응답을 돌려주도록 가변 상태로 둔다
    private var responseStatus = 200
    private var responseBody = ""

    // 운영의 Boot 자동 구성 mapper처럼 Kotlin 모듈을 ServiceLoader로 등록한다
    private val objectMapper = JsonMapper.builder().findAndAddModules().build()

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/") { exchange ->
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
            objectMapper = objectMapper,
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

    private fun call() = client.generateJudgment("system", "user", mapOf("type" to "OBJECT"))

    @Nested
    inner class 성공 {

        @Test
        fun `structured output을 LlmJudgmentDTO로 파싱한다`() {
            responseBody = envelope(
                """
                {"grade":"CAUTION","reasons":["카페인"],"items":[
                  {"emphasis":"카페인이 들어 있어요","body":"천천히 드세요."},
                  {"emphasis":"알레르기 해당 없어요","body":"포함되지 않았어요."}
                ]}
                """.trimIndent(),
            )

            val judgment = call()

            assertThat(judgment).isNotNull
            assertThat(judgment?.grade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(judgment?.items).hasSize(2)
        }

        @Test
        fun `UNKNOWN은 items 슬롯 수와 무관하게 유효하다(items는 버려진다)`() {
            responseBody = envelope("""{"grade":"UNKNOWN","reasons":[],"items":[]}""")

            assertThat(call()?.grade).isEqualTo(JudgmentGrade.UNKNOWN)
        }
    }

    @Nested
    inner class 실패 {

        @Test
        fun `HTTP 5xx면 null을 반환한다`() {
            responseStatus = 500
            responseBody = """{"error":"internal"}"""

            assertThat(call()).isNull()
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
        fun `판정 텍스트가 JSON이 아니면 null을 반환한다`() {
            responseBody = envelope("죄송하지만 판단할 수 없습니다")

            assertThat(call()).isNull()
        }

        @Test
        fun `UNKNOWN이 아닌데 items가 2개가 아니면 null을 반환한다`() {
            responseBody = envelope(
                """{"grade":"CAUTION","reasons":[],"items":[{"emphasis":"하나","body":"뿐"}]}""",
            )

            assertThat(call()).isNull()
        }
    }
}
