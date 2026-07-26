package com.gerd.domain.dictionary.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.entity.UserFoodDictionary
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DictionaryCommandService(
    private val dictionaryRepository: UserFoodDictionaryRepository,
    private val mealFoodRepository: MealFoodRepository,
    private val foodRepository: FoodRepository,
    private val userRepository: UserRepository,
    private val symptomRepository: SymptomRepository,
) {

    // 안전한 상태에 있는지 확인
    private val SAFE_STATES = setOf(SymptomState.COMFORTABLE, SymptomState.GOOD)

    fun SymptomState.isSafe() = this in SAFE_STATES

    // SymptomService.create/update와 같은 트랜잭션에서 직접 호출 — 별도 이벤트/트랜잭션 분리 없음
    @Transactional
    fun upsertSafeEntries(userId: Long, mealRecordId: Long) {
        val user = userRepository.getReferenceById(userId)
        val foodIds = mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)

        foodIds.forEach { foodId ->
            val exists = dictionaryRepository.findByUser_IdAndFood_IdAndDictionaryType(
                userId, foodId, DictionaryType.SAFE,
            ) != null
            if (exists) return@forEach

            dictionaryRepository.save(
                UserFoodDictionary(
                    user = user,
                    food = foodRepository.getReferenceById(foodId),
                    dictionaryType = DictionaryType.SAFE,
                ),
            )
        }
    }

    /**
     * 유저-음식-판정 등급에 따라 SAFE 사전 항목 제거
     * 다른 증상 기록에서 여전히 SAFE 상태로 판정되는 음식은 제거하지 않음
     */
    @Transactional
    fun removeSafeEntries(userId: Long, mealRecordId: Long) {
        val foodIds = mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)
        if (foodIds.isEmpty()) return

        val stillValidatedIds = symptomRepository
            .findFoodIdsStillSafeByOtherSymptoms(userId, foodIds, mealRecordId)
            .toSet()

        val toRemove = foodIds.filter { it !in stillValidatedIds }
        if (toRemove.isEmpty()) return

        dictionaryRepository.deleteByUserIdAndFoodIdsAndType(userId, toRemove, DictionaryType.SAFE)
    }

    /**
     * 유저-음식-판정 등급에 따라 CAUTION/RISK 사전 항목 추가
     * 이미 존재하는 경우에는 아무 작업도 하지 않음
     */
    @Transactional
    fun upsertCautionRiskEntry(userId: Long, foodId: Long, grade: JudgmentGrade) {
        val type = when (grade) {
            JudgmentGrade.CAUTION -> DictionaryType.CAUTION
            JudgmentGrade.RISK -> DictionaryType.RISK
            else -> return
        }
        if (!userRepository.existsById(userId)) throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        // 끼니 저장과 같은 트랜잭션에서 실행되므로, 동시 요청이 겹쳐도 uq_user_food_dict 충돌로
        // 전체 트랜잭션이 롤백되지 않도록 존재 확인 후 저장 대신 원자적 upsert로 처리
        dictionaryRepository.insertIfAbsent(userId, foodId, type.name)
    }
}

