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
import com.gerd.domain.streak.service.UserStreakService
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.text.Normalizer

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
    private val userStreakService: UserStreakService,
    transactionManager: PlatformTransactionManager,
) {
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
        val normalizedName = normalizeFoodName(foodName)
        val user = resolveUser(userId)
        val snapshot = loadTextJudgmentSnapshot(normalizedName, userId)
        // 음식 생성은 쓰기 tx 밖에서 — 유니크 제약 위반 시 자체 트랜잭션만 롤백돼 catch-재조회가 동작한다(같은 tx면 abort돼 후속 쿼리 실패)
        val food = resolveOrCreateUserFood(normalizedName, user)
        return writeTransactionTemplate.execute {
            saveFoodToNewMeal(food, rawEatenAt, snapshot.grade, snapshot.analysisJson, user)
        } ?: error("meal record save transaction returned null")
    }

    // 같이 먹은 끼니에 음식 추가 (ID)
    fun append(rawMealRecordId: String, foodExternalId: String, rawEatenAt: String?, userId: Long): MealFoodRecordDetailDTO {
        val food = resolveFood(foodExternalId, userId)
        val user = resolveUser(userId)
        val snapshot = loadJudgmentSnapshot(foodExternalId, userId)
        return writeTransactionTemplate.execute {
            val mealRecord = findMealRecord(rawMealRecordId, user)
            saveFoodToExistingMeal(food, rawEatenAt, mealRecord, snapshot.grade, snapshot.analysisJson, user)
        } ?: error("meal record save transaction returned null")
    }

    // 같이 먹은 끼니에 음식 추가 (text) — 캐시 우선 텍스트 판정
    fun appendByText(rawMealRecordId: String, foodName: String, rawEatenAt: String?, userId: Long): MealFoodRecordDetailDTO {
        val normalizedName = normalizeFoodName(foodName)
        val user = resolveUser(userId)
        val snapshot = loadTextJudgmentSnapshot(normalizedName, userId)
        // 음식 생성은 쓰기 tx 밖에서 (createNewByText 주석 참고)
        val food = resolveOrCreateUserFood(normalizedName, user)
        return writeTransactionTemplate.execute {
            val mealRecord = findMealRecord(rawMealRecordId, user)
            saveFoodToExistingMeal(food, rawEatenAt, mealRecord, snapshot.grade, snapshot.analysisJson, user)
        } ?: error("meal record save transaction returned null")
    }

    // 단일 음식 기록 삭제
    @Transactional
    fun delete(mealFoodId: String, userId: Long) {
        val mealFood = resolveOwnedFood(mealFoodId, userId)
        val mealRecordDbId = mealFood.mealRecord.id!!
        val isLastFood = mealFoodRepository.countByMealRecordId(mealRecordDbId) == 1L

        if (isLastFood) {
            cascadeDeleteMealRecord(mealRecordDbId, userId)
            userStreakService.refreshAfterMealDeleted(userId)
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
        userStreakService.refreshAfterMealDeleted(userId)
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
        userStreakService.updateOnMealRecorded(user.id!!, eatenAt.toLocalDate())
        val saved = mealFoodRepository.save(
            MealFood(
                user = user,
                foodId = food.id!!,
                mealRecord = mealRecord,
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
        mealRecord: MealRecord,
        grade: JudgmentGrade,
        analysisJson: String,
        user: User,
    ): MealFoodRecordDetailDTO {
        val eatenAt = mealRecordConverter.parseEatenAt(rawEatenAt)
        val saved = mealFoodRepository.save(
            MealFood(
                user = user,
                foodId = food.id!!,
                mealRecord = mealRecord,
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

    private fun findMealRecord(rawMealRecordId: String, user: User): MealRecord {
        val externalId = mealRecordConverter.parseUuid(rawMealRecordId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val userId = user.id ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        return mealRecordRepository.findByExternalIdAndUser_Id(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
    }

    // 텍스트 음식 이름 정규화 — 중복 USER 음식 생성·캐시 키 불일치 방지.
    // NFC(자모 결합 통일) → 앞뒤 공백 제거 → 내부 연속 공백 1칸으로 축소
    private fun normalizeFoodName(raw: String): String =
        Normalizer.normalize(raw, Normalizer.Form.NFC).trim().replace(WHITESPACE_RUN, " ")

    private fun resolveOrCreateUserFood(name: String, user: User): Food {
        val ownerId = user.id ?: throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        foodRepository.findByNameAndOwnerUserIdAndSource(name, ownerId, FoodSource.USER)?.let { return it }
        return try {
            foodRepository.save(
                Food(
                    name = name,
                    source = FoodSource.USER,
                    visibility = FoodVisibility.PRIVATE,
                    ownerUserId = ownerId,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            // 경합 패자 — 다른 트랜잭션이 (owner, source, name) 유니크를 먼저 차지했으므로 그 음식을 재조회해 재사용
            foodRepository.findByNameAndOwnerUserIdAndSource(name, ownerId, FoodSource.USER) ?: throw e
        }
    }

    private fun resolveOwnedFood(mealFoodId: String, userId: Long): MealFood {
        val externalId = mealRecordConverter.parseUuid(mealFoodId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
        return mealFoodRepository.findByExternalIdAndUser_Id(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
    }

    // 캐시 우선 — getJudgment 내부가 judgmentCache.get(key)로 히트 시 LLM 호출 없이 캐시값 반환.
    // 트랜잭션으로 감싸지 않는다 — LLM 호출(수 초) 동안 커넥션을 점유하지 않도록, DB 읽기는 판정 서비스가 자체 짧은 tx로 처리한다
    private fun loadJudgmentSnapshot(foodExternalId: String, userId: Long): MealJudgmentSnapshot {
        val judgment = foodJudgmentQueryService.getJudgment(foodExternalId, userId).first
        return toSnapshot(judgment.grade, judgment.items)
    }

    // 캐시 우선 — getJudgmentByText 내부가 judgmentCache.get(key)로 히트 시 LLM 호출 없이 캐시값 반환 (위와 동일하게 tx 밖에서 호출)
    private fun loadTextJudgmentSnapshot(foodName: String, userId: Long): MealJudgmentSnapshot {
        val judgment = foodJudgmentQueryService.getJudgmentByText(foodName, userId).first
        return toSnapshot(judgment.grade, judgment.items)
    }

    private fun toSnapshot(grade: JudgmentGrade, items: List<JudgmentItemDTO>): MealJudgmentSnapshot =
        MealJudgmentSnapshot(
            grade = grade,
            analysisJson = objectMapper.writeValueAsString(
                MealAnalysisSnapshotDTO(
                    judgmentGrade = grade,
                    triggerAnalysis = items.toAnalysisItem(index = 0),
                    allergyAnalysis = items.toAnalysisItem(index = 1),
                ),
            ),
        )

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

    companion object {
        private val WHITESPACE_RUN = Regex("\\s+")
    }
}
