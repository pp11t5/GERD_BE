package com.gerd.domain.meal.dto

import com.gerd.domain.symptom.entity.enums.SymptomState


data class MealFoodSummaryDTO(
    val foodName: String,
    val category: String,
    val eatenAt: String,
    // 끼니에 증상이 아직 연결되지 않았으면 null (symptom left join)
    val symptomState: SymptomState?,
)
