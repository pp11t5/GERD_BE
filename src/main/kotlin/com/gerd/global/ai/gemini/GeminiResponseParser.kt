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
        if (text.isNullOrBlank()) {
            log.warn { "Gemini 응답에 텍스트가 없습니다 (finishReason=${response.candidates.firstOrNull()?.finishReason})" }
            return null
        }
        return LlmResult(text = text, usage = response.usageMetadata?.toTokenUsage())
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
