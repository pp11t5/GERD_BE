package com.gerd.global.ai.openai

import com.gerd.global.ai.LlmClient
import com.gerd.global.ai.LlmRequest
import com.gerd.global.ai.LlmTimeoutException
import com.gerd.global.config.properties.OpenAiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.SocketTimeoutException
import java.net.http.HttpClient
import java.net.http.HttpTimeoutException
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * OpenAI Chat Completions 공통 클라이언트
 *
 * structured output(json_schema)으로 JSON 문자열만 반환
 * 도메인 DTO 파싱과 검증은 각 도메인 adapter에서 수행
 */
@Component
class OpenAiClient(
    private val openAiProperties: OpenAiProperties,
) : LlmClient {

    private val restClient = RestClient.builder()
        .baseUrl(openAiProperties.baseUrl)
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(openAiProperties.connectTimeoutMs))
                    .build(),
            ).apply { setReadTimeout(Duration.ofMillis(openAiProperties.readTimeoutMs)) },
        )
        .build()

    override fun generateJson(request: LlmRequest): String? {
        if (openAiProperties.apiKey.isBlank()) {
            log.warn { "[OpenAI] API 키가 설정되지 않아 호출을 생략합니다 (OPENAI_API_KEY)" }
            return null
        }
        return try {
            val response = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer ${openAiProperties.apiKey}")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequestBody(request))
                .retrieve()
                .body<OpenAiChatResponseDTO>()

            response?.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
        } catch (e: ResourceAccessException) {
            val cause = e.cause
            if (cause is HttpTimeoutException || cause is SocketTimeoutException) throw LlmTimeoutException(e)
            throw e
        } catch (e: Exception) {
            log.warn { "OpenAI 호출 실패: ${e.javaClass.simpleName} - ${e.message}" }
            null
        }
    }

    private fun buildRequestBody(request: LlmRequest): Map<String, Any> =
        mapOf(
            "model" to openAiProperties.model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to request.systemInstruction),
                mapOf("role" to "user", "content" to request.userContent),
            ),
            "response_format" to mapOf(
                "type" to "json_schema",
                "json_schema" to mapOf(
                    "name" to "response",
                    "strict" to true,
                    "schema" to request.responseSchema,
                ),
            ),
        )
}

data class OpenAiChatResponseDTO(
    val choices: List<ChoiceDTO> = emptyList(),
) {
    data class ChoiceDTO(val message: MessageDTO? = null)
    data class MessageDTO(val content: String? = null)
}
