package com.gerd.domain.symptom.service

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserMedicationRepository
import com.gerd.domain.onboarding.repository.UserSymptomRepository
import com.gerd.domain.onboarding.repository.UserTriggerRepository
import com.gerd.domain.symptom.dto.SymptomPatternAnalysisInputDTO
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomMealPatternRow
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 증상 패턴 분석을 위한 입력 데이터를 읽어오는 서비스
 * 1. 사용자의 최근 증상 기록과 연결된 식사 기록을 조회
 * 2. 해당 식사 기록에 포함된 음식과 음식 카테고리를 조회
 * 3. 최근 14일 동안의 증상-식사 패턴 데이터를 조회
 * 4. 위 데이터를 기반으로 SymptomPatternAnalysisInputDTO를 구성하여 반환
 */
@Component
@Transactional(readOnly = true)
class SymptomPatternAnalysisInputReader(
    private val mealRecordRepository: MealRecordRepository,
    private val mealFoodRepository: MealFoodRepository,
    private val foodRepository: FoodRepository,
    private val foodCategoryMapRepository: FoodCategoryMapRepository,
    private val userTriggerRepository: UserTriggerRepository,
    private val userAllergenRepository: UserAllergenRepository,
    private val userMedicationRepository: UserMedicationRepository,
    private val userSymptomRepository: UserSymptomRepository,
    private val symptomRepository: SymptomRepository,
) {

    fun read(symptom: Symptom, userId: Long): SymptomPatternAnalysisInputDTO {
        val mealRecordId = symptom.mealRecordId ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealRecord = mealRecordRepository.findByIdAndUser_Id(mealRecordId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealFoods = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(mealRecordId)
        val foodsById = loadFoodsById(mealFoods)
        val categoriesByFoodId = loadCategoriesByFoodId(foodsById.keys)
        val rows = symptomRepository.findLinkedRows(userId, LocalDateTime.now().minusDays(WINDOW_DAYS.toLong()))

        return SymptomPatternAnalysisInputDTO(
            user = SymptomPatternAnalysisInputDTO.UserSnapshotDTO(
                symptoms = userSymptomRepository.findByIdUserId(userId).map { it.id.symptomCode },
                triggerFoods = userTriggerRepository.findTriggerLabelsByUserId(userId).map { it.displayName },
                allergies = userAllergenRepository.findAllergensByUserId(userId).map { it.displayName },
                medications = userMedicationRepository.findByUserProfileUserId(userId).map { it.name },
            ),
            currentSymptom = SymptomPatternAnalysisInputDTO.CurrentSymptomDTO(
                symptomState = symptom.symptomState,
                symptomTypes = symptom.symptomTypes.toList(),
                occurredAt = formatDateTime(symptom.occurredAt),
                memo = symptom.memo,
            ),
            linkedMeal = buildLinkedMeal(mealRecord, mealFoods, foodsById, categoriesByFoodId),
            window = buildWindow(rows),
            features = buildFeatures(rows),
        )
    }

    private fun loadFoodsById(mealFoods: List<MealFood>): Map<Long, Food> {
        val foodIds = mealFoods.map { it.foodId }.distinct()
        if (foodIds.isEmpty()) return emptyMap()
        return foodRepository.findAllByIdsIncludingDeleted(foodIds)
            .mapNotNull { food -> food.id?.let { it to food } }
            .toMap()
    }

    private fun loadCategoriesByFoodId(foodIds: Set<Long>): Map<Long, String?> {
        if (foodIds.isEmpty()) return emptyMap()
        return foodCategoryMapRepository.findCategoryViewsByFoodIds(foodIds)
            .groupBy { it.foodId }
            .mapValues { (_, categories) -> categories.firstOrNull()?.code }
    }

    private fun buildLinkedMeal(
        mealRecord: MealRecord,
        mealFoods: List<MealFood>,
        foodsById: Map<Long, Food>,
        categoriesByFoodId: Map<Long, String?>,
    ): SymptomPatternAnalysisInputDTO.LinkedMealDTO =
        SymptomPatternAnalysisInputDTO.LinkedMealDTO(
            mealRecordId = mealRecord.externalId.toString(),
            eatenAt = formatDateTime(mealRecord.eatenAt),
            foods = mealFoods.mapNotNull { mealFood ->
                val food = foodsById[mealFood.foodId] ?: return@mapNotNull null
                SymptomPatternAnalysisInputDTO.FoodDTO(
                    name = food.name,
                    category = categoriesByFoodId[mealFood.foodId],
                    judgmentGrade = mealFood.judgedGrade?.name,
                )
            },
        )

    private fun buildWindow(rows: List<SymptomMealPatternRow>): SymptomPatternAnalysisInputDTO.WindowSummaryDTO {
        val rowsBySymptom = rows.distinctBy { it.symptomInternalId }
        val categories = rows
            .map { row -> (row.category ?: UNCATEGORIZED_CATEGORY) to row }
            .groupBy({ it.first }, { it.second })
            .map { (category, categoryRows) ->
                val distinctRows = categoryRows.distinctBy { it.symptomInternalId }
                SymptomPatternAnalysisInputDTO.CategorySummaryDTO(
                    category = category,
                    comfortCount = distinctRows.count { it.symptomState.isComfort() },
                    discomfortCount = distinctRows.count { it.symptomState.isDiscomfort() },
                )
            }
            .sortedWith(
                compareByDescending<SymptomPatternAnalysisInputDTO.CategorySummaryDTO> { it.discomfortCount }
                    .thenByDescending { it.comfortCount },
            )

        return SymptomPatternAnalysisInputDTO.WindowSummaryDTO(
            days = WINDOW_DAYS,
            linkedRecordCount = rowsBySymptom.size,
            comfortCount = rowsBySymptom.count { it.symptomState.isComfort() },
            discomfortCount = rowsBySymptom.count { it.symptomState.isDiscomfort() },
            categorySummaries = categories,
        )
    }

    private fun buildFeatures(rows: List<SymptomMealPatternRow>): SymptomPatternAnalysisInputDTO.PatternFeatureDTO {
        val summary = buildWindow(rows)
        val strongest = summary.categorySummaries.maxWithOrNull(
            compareBy<SymptomPatternAnalysisInputDTO.CategorySummaryDTO> {
                maxOf(it.comfortCount, it.discomfortCount)
            }.thenBy { it.category },
        )
        val strongestCount = strongest?.let { maxOf(it.comfortCount, it.discomfortCount) } ?: 0
        if (summary.linkedRecordCount < MIN_RELIABLE_RECORD_COUNT || strongest == null || strongestCount < MIN_RELIABLE_PATTERN_COUNT) {
            return SymptomPatternAnalysisInputDTO.PatternFeatureDTO(
                hasReliablePattern = false,
                patternCandidate = null,
                evidenceText = "최근 ${summary.days}일 연결 기록 ${summary.linkedRecordCount}건",
                consecutiveCount = null,
                labelHint = "기록 부족",
            )
        }

        val isDiscomfortPattern = strongest.discomfortCount >= strongest.comfortCount
        val labelHint = if (isDiscomfortPattern) "주의 필요" else "유지 권장"
        val patternCandidate = if (isDiscomfortPattern) {
            "${strongest.category} 식사 후 불편 기록"
        } else {
            "${strongest.category} 식사 후 편안한 기록"
        }
        val evidenceText = if (isDiscomfortPattern) {
            "${strongest.category} 식사 후 불편 기록 ${strongest.discomfortCount}건"
        } else {
            "${strongest.category} 식사 후 편안한 기록 ${strongest.comfortCount}건"
        }

        return SymptomPatternAnalysisInputDTO.PatternFeatureDTO(
            hasReliablePattern = true,
            patternCandidate = patternCandidate,
            evidenceText = evidenceText,
            consecutiveCount = strongestCount,
            labelHint = labelHint,
        )
    }

    private fun SymptomState.isComfort(): Boolean =
        this == SymptomState.COMFORTABLE || this == SymptomState.GOOD

    private fun SymptomState.isDiscomfort(): Boolean =
        this == SymptomState.UNCOMFORTABLE || this == SymptomState.SEVERE

    private fun formatDateTime(value: LocalDateTime): String =
        value.atOffset(ZoneOffset.ofHours(9)).toString()

    companion object {
        private const val WINDOW_DAYS = 14
        private const val MIN_RELIABLE_RECORD_COUNT = 3
        private const val MIN_RELIABLE_PATTERN_COUNT = 2
        private const val UNCATEGORIZED_CATEGORY = "uncategorized"
    }
}
