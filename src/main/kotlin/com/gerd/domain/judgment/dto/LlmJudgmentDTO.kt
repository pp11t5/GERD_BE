package com.gerd.domain.judgment.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade

/**
 * AI structured output 파싱 결과
 *
 * - confidence 필드 없음 — 불확실성은 신호등 등급으로 직접 표현
 * - items는 정확히 2개여야 유효 — 검증은 JudgmentGeminiAdapter가 수행
 */
data class LlmJudgmentDTO(
    val grade: JudgmentGrade,
    val personalTitle: String? = null,
    val items: List<LlmJudgmentItemDTO> = emptyList(),
    // 음식에서 추출한 트리거/알레르겐 코드 — DB 태그가 없는 텍스트 판정에서 안전 오버라이드 입력으로 사용
    // DB 음식(ID 판정)은 검수 태그를 쓰므로 이 값은 무시됨, 스키마 enum으로 코드 집합이 제한됨
    val triggerTags: List<String> = emptyList(),
    val allergenTags: List<String> = emptyList(),
    // 카테고리 미분류 음식(food.category == null)일 때만 채워짐 — 이미 분류된 음식은 null
    val categoryCode: String? = null,
) {

    data class LlmJudgmentItemDTO(
        val emphasis: String,
        val body: String,
    )
}
