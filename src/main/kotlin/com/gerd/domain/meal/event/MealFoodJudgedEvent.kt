package com.gerd.domain.meal.event

import com.gerd.domain.judgment.dto.enums.JudgmentGrade

data class MealFoodJudgedEvent(
    val userId: Long,
    val foodId: Long,
    val grade: JudgmentGrade,
)
