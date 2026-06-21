package com.gerd.domain.symptom.dto

import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType

/**
 * LLM 학습 input DTO
 */
data class SymptomPatternAnalysisInputDTO(
    val user: UserSnapshotDTO,
    val currentSymptom: CurrentSymptomDTO,
    val linkedMeal: LinkedMealDTO,
    val window: WindowSummaryDTO,
    val features: PatternFeatureDTO,
) {
    data class UserSnapshotDTO(
        val symptoms: List<String> = emptyList(),
        val triggerFoods: List<String> = emptyList(),
        val allergies: List<String> = emptyList(),
        val medications: List<String> = emptyList(),
    )

    data class CurrentSymptomDTO(
        val symptomState: SymptomState,
        val symptomTypes: List<SymptomType>,
        val occurredAt: String,
        val memo: String?,
    )

    data class LinkedMealDTO(
        val mealRecordId: String,
        val eatenAt: String,
        val foods: List<FoodDTO>,
    )

    data class FoodDTO(
        val name: String,
        val category: String?,
        val judgmentGrade: String?,
    )

    data class WindowSummaryDTO(
        val days: Int,
        val linkedRecordCount: Int,
        val comfortCount: Int,
        val discomfortCount: Int,
        val categorySummaries: List<CategorySummaryDTO> = emptyList(),
    )

    data class CategorySummaryDTO(
        val category: String,
        val comfortCount: Int,
        val discomfortCount: Int,
    )

    data class PatternFeatureDTO(
        val hasReliablePattern: Boolean,
        val patternCandidate: String?,
        val evidenceText: String?,
        val consecutiveCount: Int?,
        val labelHint: String?,
    )
}
