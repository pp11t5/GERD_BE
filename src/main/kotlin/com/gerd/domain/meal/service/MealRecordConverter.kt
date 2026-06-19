package com.gerd.domain.meal.service

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.meal.dto.MealAnalysisSnapshotDTO
import com.gerd.domain.meal.dto.MealCandidatesDTO
import com.gerd.domain.meal.dto.MealFoodRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.dto.StateRecordDTO
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

@Component
class MealRecordConverter(
    private val foodRepository: FoodRepository,
    private val foodCategoryReader: FoodCategoryReader,
    private val objectMapper: ObjectMapper,
) {

    fun toSummary(mealFood: MealFood, food: Food): MealFoodRecordDetailDTO {
        val category = foodCategoryReader.loadPrimaryByFoodIds(listOf(food.id!!))[food.id]
        return MealFoodRecordDetailDTO(
            mealFoodId = mealFood.externalId.toString(),
            eatenAt = formatEatenAt(mealFood.eatenAt),
            food = MealFoodRecordDetailDTO.FoodInfoDTO(
                mealRecordExternalId = mealFood.mealRecordId.toString(),
                name = food.name,
                category = category,
            ),
            analysis = deserializeAnalysis(mealFood.analysisJson),
            stateRecord = null,
        )
    }

    fun toDetail(mealFood: MealFood): MealFoodRecordDetailDTO {
        val food = loadFoodsIncludingDeleted(listOf(mealFood.foodId)).firstOrNull()
            ?: error("meal food ${mealFood.id} references missing food ${mealFood.foodId}")
        val category = foodCategoryReader.loadPrimaryByFoodIds(listOf(mealFood.foodId))[mealFood.foodId]
        return MealFoodRecordDetailDTO(
            mealFoodId = mealFood.externalId.toString(),
            eatenAt = formatEatenAt(mealFood.eatenAt),
            food = MealFoodRecordDetailDTO.FoodInfoDTO(
                mealRecordExternalId = mealFood.mealRecordId.toString(),
                name = food.name,
                category = category,
            ),
            analysis = deserializeAnalysis(mealFood.analysisJson),
            stateRecord = null,
        )
    }

    fun toGroupDetail(mealRecord: MealRecord, foods: List<MealFood>, symptoms: List<Symptom>): MealRecordDetailDTO {
        val foodIds = foods.map { it.foodId }.distinct()
        val foodMap = loadFoodsIncludingDeleted(foodIds).associateBy { it.id }
        val categories = foodCategoryReader.loadPrimaryByFoodIds(foodIds)
        val stateRecord = symptoms.firstOrNull()?.let { symptom ->
            val externalId = symptom.externalId ?: return@let null
            StateRecordDTO(
                stateRecordId = externalId.toString(),
                label = symptom.symptomState,
                date = formatSymptomDate(symptom.occurredAt),
                timingMinutes = Duration.between(mealRecord.eatenAt, symptom.occurredAt).toMinutes().toInt(),
            )
        }
        return MealRecordDetailDTO(
            mealRecordId = mealRecord.id.toString(),
            eatenAt = formatEatenAt(mealRecord.eatenAt),
            meals = foods.sortedBy { it.eatenAt }.map { mealFood ->
                val food = foodMap[mealFood.foodId]
                    ?: error("meal food ${mealFood.id} references missing food ${mealFood.foodId}")
                MealRecordDetailDTO.MealFoodDetailDTO(
                    mealFoodId = mealFood.externalId.toString(),
                    name = food.name,
                    category = categories[mealFood.foodId],
                    eatenAt = formatEatenAt(mealFood.eatenAt),
                )
            },
            stateRecords = stateRecord,
        )
    }

    fun toCandidates(mealRecords: List<MealRecord>, foods: List<MealFood>): List<MealCandidatesDTO> {
        val foodIds = foods.map { it.foodId }.distinct()
        val foodMap = loadFoodsIncludingDeleted(foodIds).associateBy { it.id }
        val categories = foodCategoryReader.loadPrimaryByFoodIds(foodIds)
        val foodsByRecord = foods.groupBy { it.mealRecordId }

        return mealRecords
            .sortedByDescending { it.eatenAt }
            .groupBy { it.eatenAt.atZone(SEOUL).toLocalDate().toString() }
            .map { (date, records) ->
                MealCandidatesDTO(
                    date = date,
                    meals = records.map { record ->
                        val recordFoods = (foodsByRecord[record.id] ?: emptyList()).sortedBy { it.eatenAt }
                        val firstFood = recordFoods.firstOrNull()?.let { foodMap[it.foodId] }
                        MealCandidatesDTO.MealCandidateItem(
                            mealRecordId = record.id.toString(),
                            eatenAt = formatEatenAt(record.eatenAt),
                            representativeFood = MealCandidatesDTO.RepresentativeFood(
                                name = firstFood?.name ?: "",
                                category = firstFood?.let { categories[it.id] },
                            ),
                            otherFoodCount = (recordFoods.size - 1).coerceAtLeast(0),
                        )
                    },
                )
            }
    }

    private fun deserializeAnalysis(json: String?): MealAnalysisSnapshotDTO? =
        json?.let { runCatching { objectMapper.readValue(it, MealAnalysisSnapshotDTO::class.java) }.getOrNull() }

    private fun formatSymptomDate(occurredAt: LocalDateTime): String {
        return occurredAt.atZone(SEOUL).toLocalDate().toString()
    }

    fun parseEatenAt(raw: String?): LocalDateTime =
        if (raw == null) {
            LocalDateTime.now(SEOUL)
        } else {
            try {
                OffsetDateTime.parse(raw).atZoneSameInstant(SEOUL).toLocalDateTime()
            } catch (e: DateTimeParseException) {
                throw GeneralException(MealErrorCode.INVALID_DATE_TIME)
            }
        }

    fun parseUuid(raw: String): UUID? =
        try {
            UUID.fromString(raw.trim())
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun loadFoodsIncludingDeleted(ids: Collection<Long>): List<Food> =
        if (ids.isEmpty()) emptyList() else foodRepository.findAllByIdsIncludingDeleted(ids)

    fun formatEatenAt(eatenAt: LocalDateTime): String =
        eatenAt.atZone(SEOUL).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    companion object {
        private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
