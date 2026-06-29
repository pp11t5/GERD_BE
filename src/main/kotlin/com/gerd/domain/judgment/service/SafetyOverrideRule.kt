package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import org.springframework.stereotype.Component

/**
 * 안전 오버라이드 — 룰은 강등만 하고 LLM 등급을 올리지 않는다 (spec §4 ②)
 *
 * - 알레르겐 교집합: 의학적 금기라 등급 불문 무조건 RISK
 * - 트리거 교집합: GERD 트리거는 양·조리법·개인차로 영향이 갈려 일률 RISK는 과잉 — RECOMMEND만 CAUTION으로 강등
 */
@Component
class SafetyOverrideRule {

    fun apply(
        llmGrade: JudgmentGrade,
        foodTriggers: List<TagDTO>,
        foodAllergens: List<TagDTO>,
        userTriggers: List<TagDTO>,
        userAllergens: List<TagDTO>,
    ): OverrideResult {
        val allergenMatches = matchByCode(foodAllergens, userAllergens)
        val triggerMatches = matchByCode(foodTriggers, userTriggers)

        val grade = when {
            allergenMatches.isNotEmpty() -> JudgmentGrade.RISK
            triggerMatches.isNotEmpty() && llmGrade == JudgmentGrade.RECOMMEND -> JudgmentGrade.CAUTION
            else -> llmGrade
        }
        return OverrideResult(grade, allergenMatches, triggerMatches)
    }

    private fun matchByCode(foodTags: List<TagDTO>, userTags: List<TagDTO>): List<TagDTO> {
        val userCodes = userTags.map { it.code }.toSet()
        return foodTags.filter { it.code in userCodes }
    }

    // 매치 내역은 결정적 카피 생성에 사용한다 (LLM이 언급을 놓쳐도 서버가 보장)
    data class OverrideResult(
        val grade: JudgmentGrade,
        val allergenMatches: List<TagDTO>,
        val triggerMatches: List<TagDTO>,
    )
}
