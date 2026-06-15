package com.gerd.domain.judgment.dto

// generateContent 응답에서 판정 파싱에 필요한 필드만 매핑 (나머지는 무시)
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
