package com.gerd.domain.meal.service

import com.gerd.domain.meal.dto.MealCandidatesDTO
import com.gerd.domain.meal.dto.MealFoodRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

@Service
@Transactional(readOnly = true)
class MealQueryService(
    private val mealFoodRepository: MealFoodRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val symptomRepository: SymptomRepository,
    private val mealRecordConverter: MealRecordConverter,
) {

    // 음식 기록 상세 조회
    fun getDetail(mealFoodId: String, userId: Long): MealFoodRecordDetailDTO {
        val externalId = mealRecordConverter.parseUuid(mealFoodId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
        val mealFood = mealFoodRepository.findByExternalIdAndUserId(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_FOOD_NOT_FOUND)
        return mealRecordConverter.toDetail(mealFood)
    }

    // 음식 기록 그룹 상세 조회
    fun getGroupDetail(mealRecordId: String, userId: Long): MealRecordDetailDTO {
        val id = mealRecordConverter.parseUuid(mealRecordId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val mealRecord = mealRecordRepository.findByIdAndUserId(id, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND)
        val symptoms = symptomRepository.findByMealRecordId(id)
        return mealRecordConverter.toGroupDetail(mealRecord, symptoms)
    }

    // 최근 24시간 내 기록 중 증상과 연결되지 않은 음식 기록 그룹 후보 조회
    fun getCandidates(userId: Long): List<MealCandidatesDTO> {
        val cutoff = LocalDateTime.now(SEOUL).minusHours(24)
        val mealRecords = mealRecordRepository.findByUserIdAndEatenAtAfter(userId, cutoff)
        if (mealRecords.isEmpty()) return emptyList()
        val linkedIds = symptomRepository.findLinkedMealRecordIdsByUserId(userId).toSet()
        val candidates = mealRecords.filter { it.id !in linkedIds }
        if (candidates.isEmpty()) return emptyList()
        val foods = mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(candidates.map { it.id })
        return mealRecordConverter.toCandidates(candidates, foods)
    }
}
