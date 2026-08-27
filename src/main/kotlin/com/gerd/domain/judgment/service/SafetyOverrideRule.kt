package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import org.springframework.stereotype.Component

/**
 * 안전 오버라이드 — 룰은 강등만 하고 LLM 등급을 올리지 않음
 *
 * - 치명 위험군 알레르겐 교집합: 등급 불문 RISK
 * - 그 외 알레르겐 교집합: RECOMMEND만 CAUTION으로 강등 (LLM의 기존 CAUTION/RISK는 유지)
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

        val criticalAllergenMatches = allergenMatches.filter { it.code in CRITICAL_ALLERGEN_CODES }
        val grade = when {
            criticalAllergenMatches.isNotEmpty() -> JudgmentGrade.RISK
            allergenMatches.isNotEmpty() && llmGrade == JudgmentGrade.RECOMMEND -> JudgmentGrade.CAUTION
            triggerMatches.isNotEmpty() && llmGrade == JudgmentGrade.RECOMMEND -> JudgmentGrade.CAUTION
            else -> llmGrade
        }
        return OverrideResult(grade, allergenMatches, criticalAllergenMatches, triggerMatches)
    }

    private fun matchByCode(foodTags: List<TagDTO>, userTags: List<TagDTO>): List<TagDTO> {
        val userCodes = userTags.map { it.code }.toSet()
        return foodTags.filter { it.code in userCodes }
    }

    // 매치 내역은 결정적 카피 생성에 사용 (LLM이 언급을 놓쳐도 서버가 보장)
    data class OverrideResult(
        val grade: JudgmentGrade,
        val allergenMatches: List<TagDTO>,
        val criticalAllergenMatches: List<TagDTO>,
        val triggerMatches: List<TagDTO>,
    )

    companion object {
        // 식품 알레르기 중 아나필락시스 위험이 큰 항목만 서버의 강제 RISK 대상으로 둔다.
        private val CRITICAL_ALLERGEN_CODES = setOf("peanut", "tree_nut", "crustacean", "fish_shellfish")
    }
}
