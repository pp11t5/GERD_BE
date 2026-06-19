package com.gerd.domain.meal.service

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodAccessPolicy
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.judgment.service.FoodJudgmentQueryService
import com.gerd.domain.meal.dto.MealAnalysisSnapshotDTO
import com.gerd.domain.meal.dto.MealFoodRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordAppendRequestDTO
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

@Service
class MealCommandService(
    private val mealFoodRepository: MealFoodRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val foodRepository: FoodRepository,
    private val mealRecordConverter: MealRecordConverter,
    private val foodJudgmentQueryService: FoodJudgmentQueryService,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager,
) {
    private val judgmentTransactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        isReadOnly = true
    }

    private val writeTransactionTemplate = TransactionTemplate(transactionManager)

    // 식사 기록 추가 등록
    fun create(request: MealRecordAppendRequestDTO, userId: Long): MealFoodRecordDetailDTO {
        val foodExternalId = mealRecordConverter.parseUuid(request.foodExternalId)
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        val food = foodRepository.findByExternalId(foodExternalId)
            ?.takeIf { FoodAccessPolicy.isVisibleTo(it, userId) }
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        val judgment = loadJudgmentSnapshot(request.foodExternalId, userId)
        return writeTransactionTemplate.execute {
            saveFood(food, request.eatenAt, request.mealRecordId, judgment.grade, judgment.analysisJson, userId)
        } ?: error("meal record save transaction returned null")
    }

    // 단일 음식 기록 삭제
    @Transactional
    fun delete(mealFoodId: String, userId: Long) {
        val mealFood = resolveOwnedFood(mealFoodId, userId)
        mealFoodRepository.delete(mealFood)
        mealFoodRepository.flush()
        val remainingFoodCount = mealFoodRepository.countByMealRecordId(mealFood.mealRecordId)
        if (remainingFoodCount == 0L) {
            mealRecordRepository.findByIdAndUserId(mealFood.mealRecordId, userId)
                ?.let(mealRecordRepository::delete)
        }
    }

    // 음식 기록 그룹 삭제
    @Transactional
    fun deleteMealRecord(mealRecordId: String, userId: Long) {
        val externalId = mealRecordConverter.parseUuid(mealRecordId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealRecord = mealRecordRepository.findByExternalIdAndUserId(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val foods = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(mealRecord.id!!)
        mealFoodRepository.deleteAll(foods)
        mealRecordRepository.delete(mealRecord)
    }

    // 음식 기록 저장
    private fun saveFood(
        food: Food,
        rawEatenAt: String?,
        rawMealRecordId: String?,
        judgedGrade: JudgmentGrade,
        analysisJson: String,
        userId: Long,
    ): MealFoodRecordDetailDTO {
        val eatenAt = mealRecordConverter.parseEatenAt(rawEatenAt)
        val mealRecordId = resolveMealRecordId(rawMealRecordId, userId, eatenAt)
        val saved = mealFoodRepository.save(
            MealFood(
                userId = userId,
                foodId = food.id!!,
                mealRecordId = mealRecordId,
                eatenAt = eatenAt,
                judgedGrade = judgedGrade,
                analysisJson = analysisJson,
            ),
        )
        return mealRecordConverter.toSummary(saved, food)
    }

    // 사용자 소유 기록 끼니 ID인지 검증
    private fun resolveMealRecordId(rawMealRecordId: String?, userId: Long, eatenAt: LocalDateTime): Long {
        if (rawMealRecordId == null) return mealRecordRepository.save(MealRecord(userId = userId, eatenAt = eatenAt)).id!!
        val mealRecordExternalId = mealRecordConverter.parseUuid(rawMealRecordId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealRecord = mealRecordRepository.findByExternalIdAndUserId(mealRecordExternalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        return mealRecord.id!!
    }

    // 사용자 소유 음식 기록인지 검증
    private fun resolveOwnedFood(mealFoodId: String, userId: Long): MealFood {
        val externalId = mealRecordConverter.parseUuid(mealFoodId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
        return mealFoodRepository.findByExternalIdAndUserId(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
    }

    private fun loadJudgmentSnapshot(foodExternalId: String, userId: Long): MealJudgmentSnapshot =
        judgmentTransactionTemplate.execute {
            val judgment = foodJudgmentQueryService.getJudgment(foodExternalId, userId).first
            MealJudgmentSnapshot(
                grade = judgment.grade,
                analysisJson = objectMapper.writeValueAsString(
                    MealAnalysisSnapshotDTO(
                        judgmentGrade = judgment.grade,
                        triggerAnalysis = judgment.items.toAnalysisItem(index = 0),
                        allergyAnalysis = judgment.items.toAnalysisItem(index = 1),
                    ),
                ),
            )
        } ?: error("meal judgment transaction returned null")

    private fun List<JudgmentItemDTO>.toAnalysisItem(
        index: Int,
    ): MealAnalysisSnapshotDTO.AnalysisItemDTO {
        val item = getOrNull(index)
        return MealAnalysisSnapshotDTO.AnalysisItemDTO(
            ment = item?.emphasis.orEmpty(),
            content = item?.body.orEmpty(),
        )
    }

    private data class MealJudgmentSnapshot(
        val grade: JudgmentGrade,
        val analysisJson: String,
    )
}
