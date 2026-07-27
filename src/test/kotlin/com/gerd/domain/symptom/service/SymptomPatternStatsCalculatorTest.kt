package com.gerd.domain.symptom.service

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.symptom.dto.enums.SymptomPatternLabel
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomMealPatternRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SymptomPatternStatsCalculatorTest {

    private val calculator = SymptomPatternStatsCalculator()
    private val now = LocalDateTime.of(2026, 7, 20, 12, 0)

    @Test
    fun `연결 기록이 부족하면 OBSERVING으로 판정한다`() {
        val rows = listOf(
            row(id = 1L, category = "soup_stew", state = SymptomState.COMFORTABLE),
            row(id = 2L, category = "soup_stew", state = SymptomState.COMFORTABLE),
        )

        val stats = calculator.calculate(WINDOW_DAYS, rows)
        val feature = calculator.resolveFeature(stats)

        assertThat(feature.label).isEqualTo(SymptomPatternLabel.OBSERVING)
    }

    @Test
    fun `한 카테고리에서 불편 기록이 우세하면 CAUTION으로 판정한다`() {
        val rows = listOf(
            row(id = 1L, category = "noodle", state = SymptomState.UNCOMFORTABLE),
            row(id = 2L, category = "noodle", state = SymptomState.UNCOMFORTABLE),
            row(id = 3L, category = "noodle", state = SymptomState.COMFORTABLE),
        )

        val stats = calculator.calculate(WINDOW_DAYS, rows)
        val feature = calculator.resolveFeature(stats)

        assertThat(feature.label).isEqualTo(SymptomPatternLabel.CAUTION)
        assertThat(feature.foodGroup).isEqualTo("noodle")
        assertThat(feature.repeatCount).isEqualTo(2)
    }

    @Test
    fun `한 카테고리에서 편안 기록이 우세하면 MAINTAIN으로 판정한다`() {
        val rows = listOf(
            row(id = 1L, category = "soup_stew", state = SymptomState.COMFORTABLE),
            row(id = 2L, category = "soup_stew", state = SymptomState.COMFORTABLE),
            row(id = 3L, category = "soup_stew", state = SymptomState.UNCOMFORTABLE),
        )

        val stats = calculator.calculate(WINDOW_DAYS, rows)
        val feature = calculator.resolveFeature(stats)

        assertThat(feature.label).isEqualTo(SymptomPatternLabel.MAINTAIN)
        assertThat(feature.foodGroup).isEqualTo("soup_stew")
        assertThat(feature.comfortCount).isEqualTo(2)
    }

    @Test
    fun `한 카테고리에서 편안·불편 기록 수가 같으면 CAUTION으로 판정한다`() {
        val rows = listOf(
            row(id = 1L, category = "noodle", state = SymptomState.UNCOMFORTABLE),
            row(id = 2L, category = "noodle", state = SymptomState.UNCOMFORTABLE),
            row(id = 3L, category = "noodle", state = SymptomState.COMFORTABLE),
            row(id = 4L, category = "noodle", state = SymptomState.COMFORTABLE),
        )

        val stats = calculator.calculate(WINDOW_DAYS, rows)
        val feature = calculator.resolveFeature(stats)

        assertThat(feature.label).isEqualTo(SymptomPatternLabel.CAUTION)
    }

    private fun row(
        id: Long,
        category: String?,
        state: SymptomState,
    ) = SymptomMealPatternRow(
        symptomInternalId = id,
        symptomState = state,
        occurredAt = now.minusDays(id),
        mealRecordId = id,
        foodName = "음식$id",
        category = category,
        judgmentGrade = JudgmentGrade.CAUTION,
    )

    companion object {
        private const val WINDOW_DAYS = 14
    }
}
