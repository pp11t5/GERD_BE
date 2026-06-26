package com.gerd.domain.meal.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.service.DictionaryCommandService
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
import com.gerd.domain.symptom.repository.SymptomRepository
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
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val mealRecordConverter: MealRecordConverter,
    private val foodJudgmentQueryService: FoodJudgmentQueryService,
    private val objectMapper: ObjectMapper,
    private val symptomRepository: SymptomRepository,
    private val dictionaryCommandService: DictionaryCommandService,
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
        val user = userRepository.findById(userId)
            .orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }
        val judgment = loadJudgmentSnapshot(request.foodExternalId, userId)
        return writeTransactionTemplate.execute {
            saveFood(food, request.eatenAt, request.mealRecordId, judgment.grade, judgment.analysisJson, user)
        } ?: error("meal record save transaction returned null")
    }

    // 단일 음식 기록 삭제
    @Transactional
    fun delete(mealFoodId: String, userId: Long) {
        val mealFood = resolveOwnedFood(mealFoodId, userId)
        val isLastFood = mealFoodRepository.countByMealRecordId(mealFood.mealRecordId) == 1L

        if (isLastFood) {
            // 마지막 음식이면 끼니 전체 삭제 플로우와 동일하게 순서 보장
            cascadeDeleteMealRecord(mealFood.mealRecordId, userId)
        } else {
            mealFoodRepository.delete(mealFood)
        }
    }

    // 음식 기록 그룹 삭제
    @Transactional
    fun deleteMealRecord(mealRecordId: String, userId: Long) {
        val externalId = mealRecordConverter.parseUuid(mealRecordId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealRecord = mealRecordRepository.findByExternalIdAndUser_Id(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        cascadeDeleteMealRecord(mealRecord.id!!, userId)
    }

    // 도감 SAFE 제거 →  증상 삭제 → MealFood 삭제 → MealRecord 삭제
    private fun cascadeDeleteMealRecord(mealRecordDbId: Long, userId: Long) {
        val linkedSymptoms = symptomRepository.findByMealRecordId(mealRecordDbId)

        if (linkedSymptoms.isNotEmpty()) {
            dictionaryCommandService.removeSafeEntries(userId, mealRecordDbId)
            symptomRepository.deleteAll(linkedSymptoms)
        }

        val foods = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(mealRecordDbId)
        mealFoodRepository.deleteAll(foods)

        mealRecordRepository.findByIdAndUser_Id(mealRecordDbId, userId)
            ?.let(mealRecordRepository::delete)
    }

    // 음식 기록 저장
    private fun saveFood(
        food: Food,
        rawEatenAt: String?,
        rawMealRecordId: String?,
        judgedGrade: JudgmentGrade,
        analysisJson: String,
        user: User,
    ): MealFoodRecordDetailDTO {
        val eatenAt = mealRecordConverter.parseEatenAt(rawEatenAt)
        val mealRecordId = resolveMealRecordId(rawMealRecordId, user, eatenAt)
        val saved = mealFoodRepository.save(
            MealFood(
                user = user,
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
    private fun resolveMealRecordId(rawMealRecordId: String?, user: User, eatenAt: LocalDateTime): Long {
        if (rawMealRecordId == null) return mealRecordRepository.save(MealRecord(user = user, eatenAt = eatenAt)).id!!
        val mealRecordExternalId = mealRecordConverter.parseUuid(rawMealRecordId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val userId = user.id ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        val mealRecord = mealRecordRepository.findByExternalIdAndUser_Id(mealRecordExternalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        return mealRecord.id!!
    }

    // 사용자 소유 음식 기록인지 검증
    private fun resolveOwnedFood(mealFoodId: String, userId: Long): MealFood {
        val externalId = mealRecordConverter.parseUuid(mealFoodId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
        return mealFoodRepository.findByExternalIdAndUser_Id(externalId, userId)
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
