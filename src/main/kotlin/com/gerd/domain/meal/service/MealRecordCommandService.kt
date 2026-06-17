package com.gerd.domain.meal.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodAccessPolicy
import com.gerd.domain.meal.dto.CreateMealRecordByTextRequestDTO
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
 * - 생성(ID): DB 음식 UUID로 기록 — 노출 범위 검증
 * - 생성(text): 자유 텍스트로 기록 — 동일 이름 USER 음식 재사용 또는 자동 생성
 * - 수정: 메모만 편집 가능(200자 상한)
 * - 삭제: soft delete — 재삭제 시 404(멱등 아님)
 */
@Service
class MealRecordCommandService(
    private val mealRecordRepository: MealRecordRepository,
    private val foodRepository: FoodRepository,
    private val mealRecordAssembler: MealRecordAssembler,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun create(request: CreateMealRecordRequestDTO, userId: Long): MealRecordSummaryDTO {
        val foodExternalId = mealRecordAssembler.parseUuid(request.foodExternalId)
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        val food = foodRepository.findByExternalId(foodExternalId)
            ?.takeIf { FoodAccessPolicy.isVisibleTo(it, userId) }
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)

        return saveRecord(food, request.eatenAt, request.mealGroupId, request.judgedGrade, userId)
    }

    @Transactional
    fun createByText(request: CreateMealRecordByTextRequestDTO, userId: Long): MealRecordSummaryDTO {
        val name = request.foodTextInput.trim()
        val food = foodRepository.findByNameAndOwnerUserIdAndSource(name, userId, FoodSource.USER)
            ?: foodRepository.save(
                Food(
                    name = name,
                    source = FoodSource.USER,
                    visibility = FoodVisibility.PRIVATE,
                    ownerUserId = userId,
                ),
            )

        return saveRecord(food, request.eatenAt, request.mealGroupId, request.judgedGrade, userId)
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

    private fun saveRecord(
        food: Food,
        rawEatenAt: String?,
        rawMealGroupId: String?,
        judgedGrade: com.gerd.domain.judgment.dto.enums.JudgmentGrade?,
        userId: Long,
    ): MealRecordSummaryDTO {
        val eatenAt = mealRecordAssembler.parseEatenAt(rawEatenAt)
        val mealGroupId = resolveMealGroupId(rawMealGroupId, userId)
        val user = userRepository.getReferenceById(userId)
        val saved = mealRecordRepository.save(
            MealRecord(
                user = user,
                foodId = food.id!!,
                mealGroupId = mealGroupId,
                eatenAt = eatenAt,
                judgedGrade = judgedGrade,
            ),
        )
        return mealRecordAssembler.toSummary(saved, food)
    }

    // 미전달 = 새 끼니(새 uuid), 전달 = 기존 끼니에 추가(존재·본인 소유 검증, 실패 MEAL404_2)
    private fun resolveMealGroupId(rawMealGroupId: String?, userId: Long): UUID {
        if (rawMealGroupId == null) return UUID.randomUUID()
        val mealGroupId = mealRecordAssembler.parseUuid(rawMealGroupId)
            ?: throw GeneralException(MealErrorCode.MEAL_GROUP_NOT_FOUND)
        if (!mealRecordRepository.existsByUser_IdAndMealGroupId(userId, mealGroupId)) {
            throw GeneralException(MealErrorCode.MEAL_GROUP_NOT_FOUND)
        }
        return mealGroupId
    }

    private fun resolveOwnedRecord(mealId: String, userId: Long): MealRecord {
        val externalId = mealRecordAssembler.parseUuid(mealId)
            ?: throw GeneralException(MealErrorCode.MEAL_NOT_FOUND)
        return mealRecordRepository.findByExternalIdAndUser_Id(externalId, userId)
            ?: throw GeneralException(MealErrorCode.MEAL_NOT_FOUND)
    }

    companion object {
        const val MEMO_MAX_LENGTH = 200
    }
}
