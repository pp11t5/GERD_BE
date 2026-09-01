package com.gerd.global.ai.gemini

import com.gerd.global.ai.LlmClient
import com.gerd.global.ai.LlmRequest
import com.gerd.global.ai.LlmResult
import com.gerd.global.ai.LlmTimeoutException
import com.gerd.global.config.properties.GeminiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import java.net.http.HttpClient
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * Gemini generateContent 공통 클라이언트
 *
 * 도메인별 프롬프트와 스키마를 받아 JSON 문자열만 반환
 * 도메인 DTO 파싱과 검증은 각 도메인 adapter/service에서 수행
 */
@Primary
@Component
class GeminiClient(
    private val geminiProperties: GeminiProperties,
    private val geminiResponseParser: GeminiResponseParser,
) : LlmClient {

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

    override fun generateJson(request: LlmRequest): LlmResult? {
        if (geminiProperties.apiKey.isBlank()) {
            log.warn { "[Gemini] Gemini API 키가 설정되지 않아 호출을 생략합니다 (GEMINI_API_KEY)" }
            return null
        }
        return try {
            val response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", geminiProperties.model)
                .header(API_KEY_HEADER, geminiProperties.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequestBody(request))
                .retrieve()
                .body<GeminiGenerateResponseDTO>()

            response?.let(geminiResponseParser::extractResult)
        } catch (e: HttpServerErrorException) {
            throw e
        } catch (e: ResourceAccessException) {
            // JDK HttpClient는 read timeout 시 HttpTimeoutException, 구형 클라이언트는 SocketTimeoutException 사용
            val cause = e.cause
            if (cause is HttpTimeoutException || cause is SocketTimeoutException) throw LlmTimeoutException(e)
            throw e
        } catch (e: Exception) {
            log.warn { "Gemini 호출 실패: ${e.javaClass.simpleName} - ${e.message}" }
            throw e
        }
    }

    private fun buildRequestBody(request: LlmRequest): Map<String, Any> =
        mapOf(
            "system_instruction" to mapOf("parts" to listOf(mapOf("text" to request.systemInstruction))),
            "contents" to listOf(
                mapOf("role" to "user", "parts" to listOf(mapOf("text" to request.userContent))),
            ),
            "generationConfig" to mapOf(
                "responseMimeType" to "application/json",
                "responseSchema" to request.responseSchema,
            ),
        )

    companion object {
        private const val API_KEY_HEADER = "x-goog-api-key"
    }
}
