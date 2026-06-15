package com.gerd.domain.meal.service

import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodAccessPolicy
import com.gerd.domain.meal.dto.CreateMealRecordRequestDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordSummaryDTO
import com.gerd.domain.meal.dto.UpdateMealMemoRequestDTO
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 식사 기록 생성/수정/삭제
 *
 * - 생성: food 노출 범위 검증 + 끼니 묶음 키 발급/검증, judgedGrade는 FE 전달값 그대로 저장(ADR-0017)
 * - 수정: 메모만 편집 가능(200자 상한)
 * - 삭제: soft delete — 재삭제 시 404(멱등 아님)
 */
@Service
class MealRecordCommandService(
    private val mealRecordRepository: MealRecordRepository,
    private val foodRepository: FoodRepository,
    private val mealRecordAssembler: MealRecordAssembler,
) {

    @Transactional
    fun create(request: CreateMealRecordRequestDTO, userId: Long): MealRecordSummaryDTO {
        // 형식이 잘못된 UUID는 존재하지 않는 음식과 동일하게 취급(열거 단서 차단)
        val foodExternalId = mealRecordAssembler.parseUuid(request.foodExternalId)
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        val food = foodRepository.findByExternalId(foodExternalId)
            ?.takeIf { FoodAccessPolicy.isVisibleTo(it, userId) }
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)

        val eatenAt = mealRecordAssembler.parseEatenAt(request.eatenAt)
        val mealGroupId = resolveMealGroupId(request.mealGroupId, userId)

        val saved = mealRecordRepository.save(
            MealRecord(
                userId = userId,
                foodId = food.id!!,
                mealGroupId = mealGroupId,
                eatenAt = eatenAt,
                judgedGrade = request.judgedGrade,
            ),
        )
        return mealRecordAssembler.toSummary(saved, food)
    }

    @Transactional
    fun updateMemo(mealId: String, request: UpdateMealMemoRequestDTO, userId: Long): MealRecordDetailDTO {
        val record = resolveOwnedRecord(mealId, userId)
        val trimmedMemo = request.memo?.trim()
        if (trimmedMemo != null && trimmedMemo.length > MEMO_MAX_LENGTH) {
            throw GeneralException(MealErrorCode.MEMO_TOO_LONG)
        }
        record.updateMemo(trimmedMemo)
        return mealRecordAssembler.toDetail(record)
    }

    @Transactional
    fun delete(mealId: String, userId: Long) {
        val record = resolveOwnedRecord(mealId, userId)
        mealRecordRepository.delete(record)
    }

    // 미전달 = 새 끼니(새 uuid), 전달 = 기존 끼니에 추가(존재·본인 소유 검증, 실패 MEAL404_2)
    private fun resolveMealGroupId(rawMealGroupId: String?, userId: Long): UUID {
        if (rawMealGroupId == null) return UUID.randomUUID()
        val mealGroupId = mealRecordAssembler.parseUuid(rawMealGroupId)
            ?: throw GeneralException(MealErrorCode.MEAL_GROUP_NOT_FOUND)
        if (!mealRecordRepository.existsByUserIdAndMealGroupId(userId, mealGroupId)) {
            throw GeneralException(MealErrorCode.MEAL_GROUP_NOT_FOUND)
        }
        return mealGroupId
    }

    private fun resolveOwnedRecord(mealId: String, userId: Long): MealRecord {
        val externalId = mealRecordAssembler.parseUuid(mealId)
            ?: throw GeneralException(MealErrorCode.MEAL_NOT_FOUND)
        return mealRecordRepository.findByExternalIdAndUserId(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_NOT_FOUND)
    }

    companion object {
        const val MEMO_MAX_LENGTH = 200
    }
}
