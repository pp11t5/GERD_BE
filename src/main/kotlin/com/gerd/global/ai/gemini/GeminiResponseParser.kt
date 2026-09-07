package com.gerd.global.ai.gemini

import com.gerd.global.ai.LlmResult
import com.gerd.global.ai.TokenUsage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GeminiResponseParser {

    fun extractResult(response: GeminiGenerateResponseDTO): LlmResult? {
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
        val usage = response.usageMetadata?.toTokenUsage()
        // 후보 텍스트가 없어도 응답 수준 usageMetadata는 올 수 있다 — 그 경우에도 비용 집계는 남겨야 하니 usage가 있으면 null을 반환하지 않는다
        if (text.isNullOrBlank() && usage == null) {
            log.warn { "Gemini 응답에 텍스트가 없습니다 (finishReason=${response.candidates.firstOrNull()?.finishReason})" }
            return null
        }
        return LlmResult(text = text?.takeIf { it.isNotBlank() }, usage = usage)
    }
}

data class GeminiGenerateResponseDTO(
    val candidates: List<CandidateDTO> = emptyList(),
    val usageMetadata: UsageMetadataDTO? = null,
) {

    data class CandidateDTO(
        val content: ContentDTO? = null,
        val finishReason: String? = null,
    )

    data class ContentDTO(
        val parts: List<PartDTO> = emptyList(),
    )

    data class PartDTO(
        val text: String? = null,
    )

    data class UsageMetadataDTO(
        val promptTokenCount: Int = 0,
        val candidatesTokenCount: Int = 0,
        val totalTokenCount: Int = 0,
    ) {
        fun toTokenUsage() = TokenUsage(
            promptTokens = promptTokenCount,
            completionTokens = candidatesTokenCount,
            totalTokens = totalTokenCount,
        )
    }
}
