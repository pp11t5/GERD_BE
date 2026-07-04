package com.gerd.domain.meal.entity

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.global.fixture.MealRecordFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// 끼니 대표 등급 산정 규칙 — UNKNOWN(판단 불가)은 아는 등급이 하나라도 있으면 밀리고, 단독일 때만 대표가 된다
class MealRecordGradeTest {

    private fun mealRecord() = MealRecordFixture.mealRecord()

    @Nested
    inner class `recalculateGrade(삭제 후 재산정)` {

        @Test
        fun `UNKNOWN + CAUTION 이면 CAUTION`() {
            val record = mealRecord()

            record.recalculateGrade(listOf(JudgmentGrade.UNKNOWN, JudgmentGrade.CAUTION))

            assertThat(record.grade).isEqualTo(JudgmentGrade.CAUTION)
        }

        @Test
        fun `UNKNOWN 단독이면 UNKNOWN`() {
            val record = mealRecord()

            record.recalculateGrade(listOf(JudgmentGrade.UNKNOWN))

            assertThat(record.grade).isEqualTo(JudgmentGrade.UNKNOWN)
        }

        @Test
        fun `UNKNOWN + RECOMMEND 이면 RECOMMEND (아는 등급 우선)`() {
            val record = mealRecord()

            record.recalculateGrade(listOf(JudgmentGrade.UNKNOWN, JudgmentGrade.RECOMMEND))

            assertThat(record.grade).isEqualTo(JudgmentGrade.RECOMMEND)
        }

        @Test
        fun `UNKNOWN + RISK 이면 RISK (최악 우선)`() {
            val record = mealRecord()

            record.recalculateGrade(listOf(JudgmentGrade.UNKNOWN, JudgmentGrade.RISK))

            assertThat(record.grade).isEqualTo(JudgmentGrade.RISK)
        }
    }

    @Nested
    inner class `initGrade + updateGrade(음식 추가)` {

        @Test
        fun `UNKNOWN으로 시작해 CAUTION을 추가하면 CAUTION으로 교체된다`() {
            val record = mealRecord()

            record.initGrade(JudgmentGrade.UNKNOWN)
            record.updateGrade(JudgmentGrade.CAUTION)

            assertThat(record.grade).isEqualTo(JudgmentGrade.CAUTION)
        }

        @Test
        fun `CAUTION으로 시작해 UNKNOWN을 추가해도 CAUTION을 유지한다`() {
            val record = mealRecord()

            record.initGrade(JudgmentGrade.CAUTION)
            record.updateGrade(JudgmentGrade.UNKNOWN)

            assertThat(record.grade).isEqualTo(JudgmentGrade.CAUTION)
        }

        @Test
        fun `UNKNOWN만 등록되면 UNKNOWN이 대표가 된다`() {
            val record = mealRecord()

            record.initGrade(JudgmentGrade.UNKNOWN)

            assertThat(record.grade).isEqualTo(JudgmentGrade.UNKNOWN)
        }
    }
}
