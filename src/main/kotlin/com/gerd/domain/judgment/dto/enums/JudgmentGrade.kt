package com.gerd.domain.judgment.dto.enums

/**
 * LLM이 판단한 신호등 등급
 */
enum class JudgmentGrade(val priority: Int) {
    RECOMMEND(2),
    CAUTION(1),
    RISK(0),
    UNKNOWN(3),
}
