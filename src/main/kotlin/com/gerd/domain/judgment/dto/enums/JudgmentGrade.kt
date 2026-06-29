package com.gerd.domain.judgment.dto.enums

// 신호등 판정 등급 — LLM structured output과 FE 응답이 공유하는 enum (🟢🟡🔴)
// priority: 낮을수록 나쁜 등급 (끼니 최저 등급 산정 시 minByOrNull { it.priority } 사용)
enum class JudgmentGrade(val priority: Int) {
    RECOMMEND(2),
    CAUTION(1),
    RISK(0),
}
