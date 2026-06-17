package com.gerd.domain.judgment.client

import com.gerd.domain.judgment.dto.GeminiGenerateResponseDTO
import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.global.config.properties.GeminiProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.net.http.HttpClient
import java.time.Duration

/**
 * Gemini generateContent 호출 (structured output)
 *
 * 모든 실패(429/5xx/타임아웃/스키마 불일치)는 null로 통일한다 — 호출부가 UNKNOWN 폴백으로
 * 응답하고 캐시에 남기지 않으며, Caffeine loader 안에서 예외가 전파되지 않게 하는 계약이다
 */
@Component
class GeminiClient(
    private val geminiProperties: GeminiProperties,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // RestClient는 기본 타임아웃이 없어 LLM 지연 시 요청 스레드가 무한 대기할 수 있다 — 명시 필수
    private val restClient = RestClient.builder()
        .baseUrl(geminiProperties.baseUrl)
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(geminiProperties.connectTimeoutMs))
                    .build(),
            ).apply { setReadTimeout(Duration.ofMillis(geminiProperties.readTimeoutMs)) },
        )
        .build()

    @Retryable(
        retryFor = [HttpServerErrorException::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = "#{@geminiProperties.retryDelayMs}", multiplier = 2.0),
    )
    fun generateJudgment(
        systemInstruction: String,
        userContent: String,
        responseSchema: Map<String, Any>,
    ): LlmJudgmentDTO? {
        // 키 미설정 시 실패가 뻔한 HTTP 호출로 로그를 오염시키지 않고 즉시 UNKNOWN 폴백으로 보낸다
        if (geminiProperties.apiKey.isBlank()) {
            log.warn("Gemini API 키가 설정되지 않아 판정을 생략합니다 (GEMINI_API_KEY)")
            return null
        }
        return try {
            val response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", geminiProperties.model)
                // API 키는 쿼리스트링 대신 헤더로 — 액세스 로그 유출 방지
                .header(API_KEY_HEADER, geminiProperties.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequestBody(systemInstruction, userContent, responseSchema))
                .retrieve()
                .body(GeminiGenerateResponseDTO::class.java)

            response?.let { parseJudgment(it) }
        } catch (e: HttpServerErrorException) {
            throw e  // @Retryable이 처리 — 재시도 후 소진 시 recoverJudgment로 위임
        } catch (e: Exception) {
            // Error까지 삼키지 않도록 Exception만 잡는다. 프롬프트/응답 본문은 건강정보라 로그에 남기지 않는다
            log.warn("Gemini 판정 호출 실패: {} - {}", e.javaClass.simpleName, e.message)
            null
        }
    }

    @Recover
    fun recoverJudgment(
        e: HttpServerErrorException,
        systemInstruction: String,
        userContent: String,
        responseSchema: Map<String, Any>,
    ): LlmJudgmentDTO? {
        log.warn("Gemini 판정 재시도 소진: {} - {}", e.javaClass.simpleName, e.message)
        return null
    }

    private fun buildRequestBody(
        systemInstruction: String,
        userContent: String,
        responseSchema: Map<String, Any>,
    ): Map<String, Any> =
        mapOf(
            "system_instruction" to mapOf("parts" to listOf(mapOf("text" to systemInstruction))),
            "contents" to listOf(
                mapOf("role" to "user", "parts" to listOf(mapOf("text" to userContent))),
            ),
            "generationConfig" to mapOf(
                "responseMimeType" to "application/json",
                "responseSchema" to responseSchema,
            ),
        )

    private fun parseJudgment(response: GeminiGenerateResponseDTO): LlmJudgmentDTO? {
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (text.isNullOrBlank()) {
            log.warn("Gemini 응답에 판정 텍스트가 없습니다 (finishReason={})", response.candidates.firstOrNull()?.finishReason)
            return null
        }

        val judgment = objectMapper.readValue(text, LlmJudgmentDTO::class.java)
        // UNKNOWN은 items를 버리고 고정 폴백을 쓰므로 슬롯 수 검증에서 제외한다
        if (judgment.grade != JudgmentGrade.UNKNOWN && judgment.items.size != REQUIRED_ITEM_COUNT) {
            log.warn("Gemini 판정 items 슬롯 수 불일치: {}", judgment.items.size)
            return null
        }
        return judgment
    }

    companion object {
        private const val API_KEY_HEADER = "x-goog-api-key"
        private const val REQUIRED_ITEM_COUNT = 2
    }
}
