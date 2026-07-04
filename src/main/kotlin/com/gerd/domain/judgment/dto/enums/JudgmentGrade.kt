package com.gerd.domain.judgment.dto.enums

// 신호등 판정 등급 — LLM structured output과 FE 응답이 공유하는 enum (🟢🟡🔴🩶)
// priority: 낮을수록 나쁜/우선되는 등급 (끼니 대표 등급 산정 시 minByOrNull { it.priority } 사용)
// UNKNOWN은 "판단 근거 부족"으로, 아는 등급(RECOMMEND/CAUTION/RISK)이 하나라도 있으면 밀리도록 가장 큰 priority를 갖는다.
// UNKNOWN은 시스템 폴백 전용(유저입력 음식 게이트·LLM 실패)이며 LLM structured output에는 제시하지 않는다.
enum class JudgmentGrade(val priority: Int) {
    RECOMMEND(2),
    CAUTION(1),
    RISK(0),
    UNKNOWN(3),
}
