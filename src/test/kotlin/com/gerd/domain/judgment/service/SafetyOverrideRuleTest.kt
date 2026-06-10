package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SafetyOverrideRuleTest {

    private val rule = SafetyOverrideRule()

    private val caffeine = TagDTO("caffeine", "카페인")
    private val spicy = TagDTO("spicy", "매운 음식")
    private val milk = TagDTO("milk", "우유")
    private val peanut = TagDTO("peanut", "땅콩")

    @Nested
    inner class `알레르겐 교집합` {

        @Test
        fun `알레르겐이 매치되면 등급 불문 RISK로 강등한다`() {
            JudgmentGrade.entries.forEach { llmGrade ->
                val result = rule.apply(
                    llmGrade = llmGrade,
                    foodTriggers = emptyList(),
                    foodAllergens = listOf(milk, peanut),
                    userTriggers = emptyList(),
                    userAllergens = listOf(milk),
                )

                assertThat(result.grade).isEqualTo(JudgmentGrade.RISK)
                assertThat(result.allergenMatches).containsExactly(milk)
            }
        }
    }

    @Nested
    inner class `트리거 교집합` {

        @Test
        fun `트리거가 매치되면 RECOMMEND는 CAUTION으로 강등한다`() {
            val result = rule.apply(
                llmGrade = JudgmentGrade.RECOMMEND,
                foodTriggers = listOf(caffeine),
                foodAllergens = emptyList(),
                userTriggers = listOf(caffeine, spicy),
                userAllergens = emptyList(),
            )

            assertThat(result.grade).isEqualTo(JudgmentGrade.CAUTION)
            assertThat(result.triggerMatches).containsExactly(caffeine)
        }

        @Test
        fun `트리거가 매치돼도 CAUTION과 RISK는 LLM 등급을 유지한다(승급 없음)`() {
            listOf(JudgmentGrade.CAUTION, JudgmentGrade.RISK).forEach { llmGrade ->
                val result = rule.apply(
                    llmGrade = llmGrade,
                    foodTriggers = listOf(caffeine),
                    foodAllergens = emptyList(),
                    userTriggers = listOf(caffeine),
                    userAllergens = emptyList(),
                )

                assertThat(result.grade).isEqualTo(llmGrade)
            }
        }

        @Test
        fun `트리거가 매치돼도 UNKNOWN은 유지한다(거짓 확신 방지 비대칭)`() {
            val result = rule.apply(
                llmGrade = JudgmentGrade.UNKNOWN,
                foodTriggers = listOf(caffeine),
                foodAllergens = emptyList(),
                userTriggers = listOf(caffeine),
                userAllergens = emptyList(),
            )

            assertThat(result.grade).isEqualTo(JudgmentGrade.UNKNOWN)
        }
    }

    @Nested
    inner class `교집합 없음` {

        @Test
        fun `매치가 없으면 LLM 등급을 그대로 반환한다`() {
            val result = rule.apply(
                llmGrade = JudgmentGrade.RECOMMEND,
                foodTriggers = listOf(caffeine),
                foodAllergens = listOf(milk),
                userTriggers = listOf(spicy),
                userAllergens = listOf(peanut),
            )

            assertThat(result.grade).isEqualTo(JudgmentGrade.RECOMMEND)
            assertThat(result.allergenMatches).isEmpty()
            assertThat(result.triggerMatches).isEmpty()
        }
    }
}
