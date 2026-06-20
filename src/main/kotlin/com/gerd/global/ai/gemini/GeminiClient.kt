package com.gerd.global.ai.gemini

import com.gerd.global.config.properties.GeminiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.http.HttpClient
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * Gemini generateContent 공통 클라이언트.
 *
 * 도메인별 프롬프트와 스키마를 받아 JSON 문자열만 반환한다.
 * 도메인 DTO 파싱과 검증은 각 도메인 adapter/service에서 수행한다.
 */


@Component
class GeminiClient(
    private val geminiProperties: GeminiProperties,
    private val geminiResponseParser: GeminiResponseParser,
) {

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
        retryFor = [HttpServerErrorException::class, ResourceAccessException::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = "#{@geminiProperties.retryDelayMs}", multiplier = 2.0),
    )
    fun generateJson(request: GeminiRequest): String? {
        if (geminiProperties.apiKey.isBlank()) {
            log.warn { "Gemini API 키가 설정되지 않아 호출을 생략합니다 (GEMINI_API_KEY)" }
            return null
        }
        return try {
            val response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", geminiProperties.model)
                .header(API_KEY_HEADER, geminiProperties.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequestBody(request))
                .retrieve()
                .body(GeminiGenerateResponseDTO::class.java)

            response?.let(geminiResponseParser::extractText)
        } catch (e: HttpServerErrorException) {
            throw e
        } catch (e: ResourceAccessException) {
            throw e
        } catch (e: Exception) {
            log.warn { "Gemini 호출 실패: ${e.javaClass.simpleName} - ${e.message}" }
            null
        }
    }

    @Recover
    fun recoverJson(e: RestClientException, request: GeminiRequest): String? {
        log.warn { "Gemini 재시도 소진: ${e.javaClass.simpleName} - ${e.message}" }
        return null
    }

    private fun buildRequestBody(request: GeminiRequest): Map<String, Any> =
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
