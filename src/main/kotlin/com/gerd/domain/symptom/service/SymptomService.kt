package com.gerd.domain.symptom.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.dto.SymptomCreateRequestDTO
import com.gerd.domain.symptom.dto.SymptomMemoUpdateRequestDTO
import com.gerd.domain.symptom.dto.SymptomResponseDTO
import com.gerd.domain.symptom.dto.SymptomUpdateRequestDTO
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.exception.SymptomErrorCode
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.apiPayload.code.CommonErrorCode
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
) {

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
        val mealRecordId = resolveMealRecordId(
            request.mealRecordId ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND),
            userId,
        )
        val symptom = Symptom(
            user = user,
            symptomState = request.symptomState ?: throw GeneralException(CommonErrorCode.INVALID_REQUEST),
            symptomTypes = request.symptomTypes,
            occurredAt = symptomConverter.parseOccurredAt(request.occurredAt),
            mealRecordId = mealRecordId,
            memo = request.memo,
        )
        val saved = symptomRepository.save(symptom)
        // 증상 생성 후 분석이 필요하므로, 커밋 이후 비동기 갱신 예약
        scheduleAnalysisRefreshAfterCommit(saved, userId)
        return symptomConverter.toResponse(saved, buildLinkedMeal(saved, userId))

    }


    @Transactional
    fun update(symptomId: String, request: SymptomUpdateRequestDTO, userId: Long) {
        val symptom = resolveSymptom(symptomId, userId)
        val mealRecordId = resolveMealRecordId(
            request.mealRecordId ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND),
            userId,
        )
        symptom.update(
            symptomState = request.symptomState ?: throw GeneralException(SymptomErrorCode.SYMPTOM_NOT_FOUND),
            symptomTypes = request.symptomTypes,
            occurredAt = symptomConverter.parseOccurredAt(request.occurredAt),
            mealRecordId = mealRecordId,
            memo = request.memo,
        )
        scheduleAnalysisRefreshAfterCommit(symptom, userId)
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
        symptomRepository.delete(symptom)
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
        // UUID 파싱
        val symptomExternalId = parseUuid(rawSymptomId)
            ?: throw GeneralException(SymptomErrorCode.SYMPTOM_NOT_FOUND)
        // UUID로 증상 기록 조회
        return symptomRepository.findByExternalIdAndUser_Id(symptomExternalId, userId)
            ?: throw GeneralException(SymptomErrorCode.SYMPTOM_NOT_FOUND)
    }

    // 연결된 식사 기록과 음식 정보를 조회하여 DTO로 변환
    private fun buildLinkedMeal(symptom: Symptom, userId: Long): SymptomResponseDTO.LinkedMealDTO {
        val mealRecord = mealRecordRepository.findByIdAndUser_Id(symptom.mealRecordId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealFoods = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(symptom.mealRecordId)
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
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            symptomPatternRefreshService.refreshAsync(symptomId, userId)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    symptomPatternRefreshService.refreshAsync(symptomId, userId)
                }
            },
        )
    }
}
