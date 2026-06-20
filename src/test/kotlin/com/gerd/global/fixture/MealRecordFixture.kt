package com.gerd.global.fixture

import com.gerd.domain.auth.entity.User
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.entity.MealRecord
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

object MealRecordFixture {

    val MEAL_FOOD_EXTERNAL_ID: UUID = UUID.fromString("7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    const val MEAL_RECORD_ID: Long = 10L
    val MEAL_RECORD_EXTERNAL_ID: UUID = UUID.fromString("c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val EATEN_AT: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 30, 0)

    fun mealRecord(
        id: Long = MEAL_RECORD_ID,
        user: User = UserFixture.user(),
        eatenAt: LocalDateTime = EATEN_AT,
    ): MealRecord = MealRecord(
        user = user,
        eatenAt = eatenAt,
    ).apply {
        ReflectionTestUtils.setField(this, "id", id)
        externalId = MEAL_RECORD_EXTERNAL_ID
    }

    fun mealFood(
        id: Long = 1L,
        user: User = UserFixture.user(),
        foodId: Long = 1L,
        mealRecordId: Long = MEAL_RECORD_ID,
        eatenAt: LocalDateTime = EATEN_AT,
        judgedGrade: JudgmentGrade? = JudgmentGrade.RECOMMEND,
        analysisJson: String? = null,
        externalId: UUID = MEAL_FOOD_EXTERNAL_ID,
    ): MealFood = MealFood(
        user = user,
        foodId = foodId,
        mealRecordId = mealRecordId,
        eatenAt = eatenAt,
        judgedGrade = judgedGrade,
        analysisJson = analysisJson,
    ).apply {
        ReflectionTestUtils.setField(this, "id", id)
        this.externalId = externalId
    }
}
