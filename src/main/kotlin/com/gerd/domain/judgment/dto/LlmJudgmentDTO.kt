package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade

/**
 * Gemini structured output 파싱 결과 (spec §5)
 *
 * - confidence 필드 없음 — 불확실성은 신호등 등급으로 직접 표현 (ADR-0013)
 * - items는 정확히 2개여야 유효 — 검증은 JudgmentGeminiAdapter가 수행
 */
data class LlmJudgmentDTO(
    val grade: JudgmentGrade,
    // 결과 카드 상단 한 줄 제목 — 누락/공백이면 Assembler가 등급별 고정 제목으로 폴백한다
    val personalTitle: String? = null,
    val items: List<LlmJudgmentItemDTO> = emptyList(),
    // 음식에서 추출한 트리거/알레르겐 코드 — DB 태그가 없는 텍스트 판정에서 안전 오버라이드(②) 입력으로 쓴다.
    // DB 음식(ID 판정)은 검수 태그를 쓰므로 이 값은 무시된다. 스키마 enum으로 코드 집합이 제한된다.
    val triggerTags: List<String> = emptyList(),
    val allergenTags: List<String> = emptyList(),
) {

    data class LlmJudgmentItemDTO(
        val emphasis: String,
        val body: String,
    )
}
