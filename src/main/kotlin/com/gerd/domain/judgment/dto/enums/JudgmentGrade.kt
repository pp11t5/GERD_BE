package com.gerd.domain.judgment.dto.enums

// 신호등 판정 등급 — LLM structured output과 FE 응답이 공유하는 enum (🟢🟡🔴⚪)
enum class JudgmentGrade {
    RECOMMEND,
    CAUTION,
    RISK,
    UNKNOWN,
}
