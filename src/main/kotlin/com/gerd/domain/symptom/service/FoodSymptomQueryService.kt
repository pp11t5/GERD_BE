package com.gerd.domain.symptom.service

import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodAccessPolicy
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.dto.FoodSymptomResponseDTO
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FoodSymptomQueryService(
    private val foodRepository: FoodRepository,
    private val symptomRepository: SymptomRepository,
    private val mealRecordRepository: MealRecordRepository,
) {

    fun getSymptoms(foodExternalId: String, userId: Long): List<FoodSymptomResponseDTO> {
        val foodId = resolveFoodId(foodExternalId, userId)
        val symptoms = symptomRepository.findLinkedSymptomsByUserIdAndFoodId(userId, foodId)
        if (symptoms.isEmpty()) return emptyList()

        val mealRecordsById = mealRecordRepository.findAllById(symptoms.mapNotNull { it.mealRecordId }.distinct())
            .associateBy { requireNotNull(it.id) { "MealRecord.id must not be null" } }

        return symptoms.mapNotNull { symptom ->
            val mealRecordId = symptom.mealRecordId ?: return@mapNotNull null
            val mealRecord = mealRecordsById[mealRecordId] ?: return@mapNotNull null
            FoodSymptomResponseDTO(
                symptomId = symptom.externalId?.toString() ?: return@mapNotNull null,
                symptomState = symptom.symptomState,
                symptomTypes = symptom.symptomTypes.toList(),
                occurredAt = symptom.occurredAt.toString(),
                mealRecordId = mealRecord.externalId?.toString() ?: return@mapNotNull null,
                afterMealMinutes = ChronoUnit.MINUTES.between(mealRecord.eatenAt, symptom.occurredAt).toInt(),
            )
        }
    }

    private fun resolveFoodId(foodExternalId: String, userId: Long): Long {
        val externalId = parseUuid(foodExternalId)
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        val food = foodRepository.findByExternalId(externalId)
            ?.takeIf { FoodAccessPolicy.isVisibleTo(it, userId) }
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        return food.id ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
    }

    private fun parseUuid(value: String): UUID? =
        runCatching { UUID.fromString(value.trim()) }.getOrNull()
}
