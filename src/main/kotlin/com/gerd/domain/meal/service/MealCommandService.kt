package com.gerd.domain.meal.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.service.DictionaryCommandService
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodAccessPolicy
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.JudgmentItemDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.judgment.service.FoodJudgmentQueryService
import com.gerd.domain.meal.dto.MealAnalysisSnapshotDTO
import com.gerd.domain.meal.dto.MealFoodRecordDetailDTO
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

    // 신규 끼니 + 음식 추가 (ID)
    fun createNew(foodExternalId: String, rawEatenAt: String?, userId: Long): MealFoodRecordDetailDTO {
        val food = resolveFood(foodExternalId, userId)
        val user = resolveUser(userId)
        val snapshot = loadJudgmentSnapshot(foodExternalId, userId)
        return writeTransactionTemplate.execute {
            saveFoodToNewMeal(food, rawEatenAt, snapshot.grade, snapshot.analysisJson, user)
        } ?: error("meal record save transaction returned null")
    }

    // 신규 끼니 + 음식 추가 (text) — 캐시 우선 텍스트 판정
    fun createNewByText(foodName: String, rawEatenAt: String?, userId: Long): MealFoodRecordDetailDTO {
        val user = resolveUser(userId)
        val snapshot = loadTextJudgmentSnapshot(foodName, userId)
        return writeTransactionTemplate.execute {
            val food = resolveOrCreateUserFood(foodName, user)
            saveFoodToNewMeal(food, rawEatenAt, snapshot.grade, snapshot.analysisJson, user)
        } ?: error("meal record save transaction returned null")
    }

    // 같이 먹은 끼니에 음식 추가 (ID)
    fun append(rawMealRecordId: String, foodExternalId: String, rawEatenAt: String?, userId: Long): MealFoodRecordDetailDTO {
        val food = resolveFood(foodExternalId, userId)
        val user = resolveUser(userId)
        val snapshot = loadJudgmentSnapshot(foodExternalId, userId)
        return writeTransactionTemplate.execute {
            val mealRecordDbId = findMealRecordId(rawMealRecordId, user)
            saveFoodToExistingMeal(food, rawEatenAt, mealRecordDbId, snapshot.grade, snapshot.analysisJson, user)
        } ?: error("meal record save transaction returned null")
    }

    // 같이 먹은 끼니에 음식 추가 (text) — 캐시 우선 텍스트 판정
    fun appendByText(rawMealRecordId: String, foodName: String, rawEatenAt: String?, userId: Long): MealFoodRecordDetailDTO {
        val user = resolveUser(userId)
        val snapshot = loadTextJudgmentSnapshot(foodName, userId)
        return writeTransactionTemplate.execute {
            val food = resolveOrCreateUserFood(foodName, user)
            val mealRecordDbId = findMealRecordId(rawMealRecordId, user)
            saveFoodToExistingMeal(food, rawEatenAt, mealRecordDbId, snapshot.grade, snapshot.analysisJson, user)
        } ?: error("meal record save transaction returned null")
    }

    // 단일 음식 기록 삭제
    @Transactional
    fun delete(mealFoodId: String, userId: Long) {
        val mealFood = resolveOwnedFood(mealFoodId, userId)
        val isLastFood = mealFoodRepository.countByMealRecordId(mealFood.mealRecordId) == 1L

        if (isLastFood) {
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

    // 도감 SAFE 제거 → 증상 삭제 → MealFood 삭제 → MealRecord 삭제
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

    private fun saveFoodToNewMeal(
        food: Food,
        rawEatenAt: String?,
        grade: JudgmentGrade,
        analysisJson: String,
        user: User,
    ): MealFoodRecordDetailDTO {
        val eatenAt = mealRecordConverter.parseEatenAt(rawEatenAt)
        val mealRecord = mealRecordRepository.save(MealRecord(user = user, eatenAt = eatenAt))
        val saved = mealFoodRepository.save(
            MealFood(
                user = user,
                foodId = food.id!!,
                mealRecordId = mealRecord.id!!,
                eatenAt = eatenAt,
                judgedGrade = grade,
                analysisJson = analysisJson,
            ),
        )
        return mealRecordConverter.toSummary(saved, food)
    }

    private fun saveFoodToExistingMeal(
        food: Food,
        rawEatenAt: String?,
        mealRecordDbId: Long,
        grade: JudgmentGrade,
        analysisJson: String,
        user: User,
    ): MealFoodRecordDetailDTO {
        val eatenAt = mealRecordConverter.parseEatenAt(rawEatenAt)
        val saved = mealFoodRepository.save(
            MealFood(
                user = user,
                foodId = food.id!!,
                mealRecordId = mealRecordDbId,
                eatenAt = eatenAt,
                judgedGrade = grade,
                analysisJson = analysisJson,
            ),
        )
        return mealRecordConverter.toSummary(saved, food)
    }

    private fun resolveFood(foodExternalId: String, userId: Long): Food {
        val externalId = mealRecordConverter.parseUuid(foodExternalId)
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        return foodRepository.findByExternalId(externalId)
            ?.takeIf { FoodAccessPolicy.isVisibleTo(it, userId) }
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
    }

    private fun resolveUser(userId: Long): User =
        userRepository.findById(userId).orElseThrow { GeneralException(AuthErrorCode.USER_NOT_FOUND) }

    private fun findMealRecordId(rawMealRecordId: String, user: User): Long {
        val externalId = mealRecordConverter.parseUuid(rawMealRecordId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val userId = user.id ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        val mealRecord = mealRecordRepository.findByExternalIdAndUser_Id(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        return mealRecord.id!!
    }

    private fun resolveOrCreateUserFood(name: String, user: User): Food {
        val ownerId = user.id ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        return foodRepository.findByNameAndOwnerUserIdAndSource(name, ownerId, FoodSource.USER)
            ?: foodRepository.save(
                Food(
                    name = name,
                    source = FoodSource.USER,
                    visibility = FoodVisibility.PRIVATE,
                    ownerUserId = ownerId,
                ),
            )
    }

    private fun resolveOwnedFood(mealFoodId: String, userId: Long): MealFood {
        val externalId = mealRecordConverter.parseUuid(mealFoodId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
        return mealFoodRepository.findByExternalIdAndUser_Id(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
    }

    // 캐시 우선 — getJudgment 내부가 judgmentCache.get(key)로 히트 시 LLM 호출 없이 캐시값 반환
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

    // 캐시 우선 — getJudgmentByText 내부가 judgmentCache.get(key)로 히트 시 LLM 호출 없이 캐시값 반환
    private fun loadTextJudgmentSnapshot(foodName: String, userId: Long): MealJudgmentSnapshot =
        judgmentTransactionTemplate.execute {
            val judgment = foodJudgmentQueryService.getJudgmentByText(foodName, userId).first
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

    private fun List<JudgmentItemDTO>.toAnalysisItem(index: Int): MealAnalysisSnapshotDTO.AnalysisItemDTO {
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
