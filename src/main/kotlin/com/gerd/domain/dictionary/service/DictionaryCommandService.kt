package com.gerd.domain.dictionary.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
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
    private val userRepository: UserRepository,
    private val symptomRepository: SymptomRepository,
) {

    private val SAFE_STATES = setOf(SymptomState.COMFORTABLE, SymptomState.GOOD)

    @Transactional
    fun upsertFromSymptom(userId: Long, mealRecordId: Long, symptomState: SymptomState) {
        if (symptomState !in SAFE_STATES) return
        val foodIds = mealFoodRepository.findFoodIdsByMealRecordId(mealRecordId)
        if (foodIds.isEmpty()) return
        if (!userRepository.existsById(userId)) throw GeneralException(AuthErrorCode.USER_NOT_FOUND)

        foodIds.forEach { foodId ->
            dictionaryRepository.upsertType(userId, foodId, DictionaryType.SAFE.name)
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
     * 음식 판정 결과로 도감 항목을 새로 만들거나 기존 타입을 변경한다.
     */
    @Transactional
    fun upsertFromJudgment(userId: Long, foodId: Long, grade: JudgmentGrade) {
        val type = when (grade) {
            JudgmentGrade.CAUTION -> DictionaryType.CAUTION
            JudgmentGrade.RISK -> DictionaryType.RISK
            JudgmentGrade.RECOMMEND, JudgmentGrade.UNKNOWN -> return
        }
        if (!userRepository.existsById(userId)) throw GeneralException(AuthErrorCode.USER_NOT_FOUND)
        dictionaryRepository.upsertType(userId, foodId, type.name)
    }
}
