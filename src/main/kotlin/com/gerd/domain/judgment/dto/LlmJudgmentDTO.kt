package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade

/**
 * Gemini structured output 파싱 결과 (spec §5)
 *
 * - confidence 필드 없음 — 불확실성은 LLM이 grade=UNKNOWN으로 직접 표현 (ADR-0013)
 * - items는 정확히 2개여야 유효 — 검증은 GeminiClient가 수행
 */
data class LlmJudgmentDTO(
    val grade: JudgmentGrade,
    val reasons: List<String> = emptyList(),
    val items: List<LlmJudgmentItemDTO> = emptyList(),
) {

    data class LlmJudgmentItemDTO(
        val emphasis: String,
        val body: String,
    )
}
