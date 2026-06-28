package com.gerd.domain.symptom.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.service.DictionaryCommandService
import com.gerd.domain.dictionary.service.isSafe
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.streak.service.UserStreakService
import com.gerd.domain.symptom.dto.SymptomCreateRequestDTO
import com.gerd.domain.symptom.dto.SymptomMemoUpdateRequestDTO
import com.gerd.domain.symptom.dto.SymptomResponseDTO
import com.gerd.domain.symptom.dto.SymptomUpdateRequestDTO
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.exception.SymptomErrorCode
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.apiPayload.code.CommonErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Service
@Transactional(readOnly = true)
class SymptomService(
    private val symptomRepository: SymptomRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val mealFoodRepository: MealFoodRepository,
    private val foodRepository: FoodRepository,
    private val foodCategoryMapRepository: FoodCategoryMapRepository,
    private val symptomConverter: SymptomConverter,
    private val userRepository: UserRepository,
    private val symptomPatternRefreshService: SymptomPatternRefreshService,
    private val dictionaryCommandService: DictionaryCommandService,
    private val userStreakService: UserStreakService,
) {

    private val log = LoggerFactory.getLogger(SymptomService::class.java)

    // 상세 조회
    fun getDetail(symptomId: String, userId: Long): SymptomResponseDTO {
        val symptom = resolveSymptom(symptomId, userId)
        // 증상 상세 조회 시점에 분석이 최신 상태가 아닐 수 있으므로, 필요 시 비동기 갱신 예약
        if (symptom.isAnalysisDirty) symptom.externalId?.let { symptomPatternRefreshService.refreshAsync(it.toString(), userId) }
        return symptomConverter.toResponse(symptom, buildLinkedMeal(symptom, userId))
    }

    // 증상 기록 생성
    @Transactional
    fun create(userId: Long, request: SymptomCreateRequestDTO): SymptomResponseDTO {
        val user = userRepository.getReferenceById(userId)
        val mealRecordId: Long? = request.mealRecordId?.let { resolveMealRecordId(it, userId) }
        val symptom = Symptom(
            user = user,
            symptomState = request.symptomState ?: throw GeneralException(CommonErrorCode.INVALID_REQUEST),
            symptomTypes = request.symptomTypes,
            occurredAt = symptomConverter.parseOccurredAt(request.occurredAt),
            mealRecordId = mealRecordId,
            memo = request.memo,
        )
        val saved = symptomRepository.save(symptom)
        if (saved.symptomState.isStreakTarget()) {
            userStreakService.updateOnComfortableRecorded(userId, saved.occurredAt.toLocalDate())
        }
        if (mealRecordId != null) scheduleAnalysisRefreshAfterCommit(saved, userId)

        if (request.symptomState?.isSafe() == true && mealRecordId != null) {
            registerAfterCommit { dictionaryCommandService.upsertSafeEntries(userId, mealRecordId) }
        }

        return symptomConverter.toResponse(saved, buildLinkedMeal(saved, userId))
    }

    @Transactional
    fun update(symptomId: String, request: SymptomUpdateRequestDTO, userId: Long) {
        val symptom = resolveSymptom(symptomId, userId)
        val previousStreakTarget = symptom.symptomState.isStreakTarget()
        val previousDate = symptom.occurredAt.toLocalDate()
        val mealRecordId: Long? = request.mealRecordId?.let { resolveMealRecordId(it, userId) }

        symptom.mealRecordId?.let { dictionaryCommandService.removeSafeEntries(userId, it) }

        symptom.update(
            symptomState = request.symptomState ?: throw GeneralException(CommonErrorCode.INVALID_REQUEST),
            symptomTypes = request.symptomTypes,
            occurredAt = symptomConverter.parseOccurredAt(request.occurredAt),
            mealRecordId = mealRecordId,
            memo = request.memo,
        )
        val currentStreakTarget = symptom.symptomState.isStreakTarget()
        val currentDate = symptom.occurredAt.toLocalDate()
        if ((previousStreakTarget || currentStreakTarget) &&
            (previousStreakTarget != currentStreakTarget || previousDate != currentDate)
        ) {
            userStreakService.rebuildCurrentStreak(userId)
        }
        if (mealRecordId != null) scheduleAnalysisRefreshAfterCommit(symptom, userId)

        if (request.symptomState?.isSafe() == true && mealRecordId != null) {
            registerAfterCommit { dictionaryCommandService.upsertSafeEntries(userId, mealRecordId) }
        }
    }

    // 메모 업데이트
    @Transactional
    fun updateMemo(symptomId: String, request: SymptomMemoUpdateRequestDTO, userId: Long) {
        val symptom = resolveSymptom(symptomId, userId)
        symptom.updateMemo(request.memo)
        scheduleAnalysisRefreshAfterCommit(symptom, userId)
    }

    // 기록 삭제
    @Transactional
    fun delete(symptomId: String, userId: Long) {
        val symptom = resolveSymptom(symptomId, userId)
        val wasStreakTarget = symptom.symptomState.isStreakTarget()
        symptom.mealRecordId?.let { dictionaryCommandService.removeSafeEntries(userId, it) }
        symptomRepository.delete(symptom)
        if (wasStreakTarget) {
            userStreakService.rebuildCurrentStreak(userId)
        }
    }

    // 연결된 식사 기록 ID를 UUID 문자열로 받아서 내부 Long ID로 변환
    private fun resolveMealRecordId(rawMealRecordId: String, userId: Long): Long {
        val mealRecordExternalId = parseUuid(rawMealRecordId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealRecord = mealRecordRepository.findByExternalIdAndUser_Id(mealRecordExternalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        return mealRecord.id ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
    }

    // UUID 문자열을 파싱하여 UUID 객체로 변환, 실패 시 null 반환
    private fun parseUuid(raw: String): UUID? =
        runCatching { UUID.fromString(raw) }.getOrNull()

    // 증상 기록 ID를 UUID 문자열로 받아서 내부 Symptom 엔티티로 변환
    private fun resolveSymptom(rawSymptomId: String, userId: Long): Symptom {
        val symptomExternalId = parseUuid(rawSymptomId)
            ?: throw GeneralException(SymptomErrorCode.SYMPTOM_NOT_FOUND)
        return symptomRepository.findByExternalIdAndUser_Id(symptomExternalId, userId)
            ?: throw GeneralException(SymptomErrorCode.SYMPTOM_NOT_FOUND)
    }

    // 연결된 식사 기록과 음식 정보를 조회하여 DTO로 변환 (미연결 증상은 null 반환)
    private fun buildLinkedMeal(symptom: Symptom, userId: Long): SymptomResponseDTO.LinkedMealDTO? {
        val mealRecordId = symptom.mealRecordId ?: return null
        val mealRecord = mealRecordRepository.findByIdAndUser_Id(mealRecordId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealFoods = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(mealRecordId)
        val foodsById = loadFoodsById(mealFoods)
        val categoriesByFoodId = loadCategoriesByFoodId(foodsById.keys)

        return SymptomResponseDTO.LinkedMealDTO(
            mealRecordId = mealRecord.externalId?.toString()
                ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND),
            foods = mealFoods.mapNotNull { mealFood ->
                val food = foodsById[mealFood.foodId] ?: return@mapNotNull null
                SymptomResponseDTO.LinkedFoodDTO(
                    mealFoodId = mealFood.externalId?.toString() ?: return@mapNotNull null,
                    name = food.name,
                    category = categoriesByFoodId[mealFood.foodId],
                )
            },
        )
    }

    // 음식 가져오기
    private fun loadFoodsById(mealFoods: List<MealFood>): Map<Long, Food> {
        val foodIds = mealFoods.map { it.foodId }.distinct()
        if (foodIds.isEmpty()) return emptyMap()
        return foodRepository.findAllByIdsIncludingDeleted(foodIds)
            .mapNotNull { food -> food.id?.let { it to food } }
            .toMap()
    }

    // 카테고리 조회
    private fun loadCategoriesByFoodId(foodIds: Set<Long>): Map<Long, String?> {
        if (foodIds.isEmpty()) return emptyMap()
        return foodCategoryMapRepository.findCategoryViewsByFoodIds(foodIds)
            .groupBy { it.foodId }
            .mapValues { (_, categories) -> categories.firstOrNull()?.code }
    }

    // 트랜잭션 커밋 이후에 증상 패턴 분석 갱신을 예약
    private fun scheduleAnalysisRefreshAfterCommit(symptom: Symptom, userId: Long) {
        val symptomId = symptom.externalId?.toString() ?: return
        registerAfterCommit { symptomPatternRefreshService.refreshAsync(symptomId, userId) }
    }

    private fun registerAfterCommit(action: () -> Unit) {
        val wrapped = {
            try { action() }
            catch (e: Exception) {
                log.error("[afterCommit] 커밋 후 작업 실패", e)
            }
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            wrapped(); return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = wrapped()
        })
    }
}

private fun SymptomState.isStreakTarget(): Boolean =
    this == SymptomState.COMFORTABLE
