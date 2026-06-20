package com.gerd.global.ai.gemini

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GeminiResponseParser {

    fun extractText(response: GeminiGenerateResponseDTO): String? {
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (text.isNullOrBlank()) {
            log.warn { "Gemini 응답에 텍스트가 없습니다 (finishReason=${response.candidates.firstOrNull()?.finishReason})" }
            return null
        }
        return text
    }
}

data class GeminiGenerateResponseDTO(
    val candidates: List<CandidateDTO> = emptyList(),
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
}
