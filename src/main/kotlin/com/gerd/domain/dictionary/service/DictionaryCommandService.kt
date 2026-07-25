package com.gerd.domain.dictionary.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.entity.UserFoodDictionary
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class DictionaryCommandService(
    private val dictionaryRepository: UserFoodDictionaryRepository,
    private val mealFoodRepository: MealFoodRepository,
    private val foodRepository: FoodRepository,
    private val userRepository: UserRepository,
    private val symptomRepository: SymptomRepository,
) {

    // afterCommit(SymptomService.create/update)에서만 호출 — 원 트랜잭션이 이미 커밋된 시점이라
    // REQUIRED로는 커밋된 트랜잭션에 참여해 INSERT가 유실된다. 독립 트랜잭션으로 분리해 커밋을 보장한다.
    /**
     * 안전한 음식일 경우 도감에 추가
     * - 이미 존재하는 경우에는 추가하지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun upsertSafeEntries(userId: Long, mealRecordId: Long) {
        val user = userRepository.getReferenceById(userId)
        val mealFoods = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(mealRecordId)

        mealFoods.forEach { mealFood ->
            val exists = dictionaryRepository.findByUser_IdAndFood_IdAndDictionaryType(
                userId, mealFood.foodId, DictionaryType.SAFE,
            ) != null
            if (exists) return@forEach

            dictionaryRepository.save(
                UserFoodDictionary(
                    user = user,
                    food = foodRepository.getReferenceById(mealFood.foodId),
                    dictionaryType = DictionaryType.SAFE,
                ),
            )
        }
    }

    /**
     * 식사 기록에서 안정으로 판정된 음식 제거
     * 식사 기록에서 안전 판정된 음식 중, 다른 증상 기록에서 여전히 안전한 음식은 남기고 제거
     */
    @Transactional
    fun removeSafeEntries(userId: Long, mealRecordId: Long) {
        val foodIds = mealFoodRepository.findByMealRecordIdOrderByEatenAtAsc(mealRecordId)
            .map { it.foodId }
        if (foodIds.isEmpty()) return

        val stillValidatedIds = symptomRepository
            .findFoodIdsStillSafeByOtherSymptoms(userId, foodIds, mealRecordId)
            .toSet()

        val toRemove = foodIds.filter { it !in stillValidatedIds }
        if (toRemove.isEmpty()) return

        dictionaryRepository.deleteByUserIdAndFoodIdsAndType(userId, toRemove, DictionaryType.SAFE)
    }

    /**
     * 신호등 판정 시 위험,주의 음식은 도감에 추가
     * - 이미 존재하는 경우에는 추가하지 않음
     */
    @Transactional
    fun upsertCautionRiskEntry(userId: Long, foodId: Long, grade: JudgmentGrade) {
        val type = when (grade) {
            JudgmentGrade.CAUTION -> DictionaryType.CAUTION
            JudgmentGrade.RISK -> DictionaryType.RISK
            else -> return
        }
        val exists = dictionaryRepository.findByUser_IdAndFood_IdAndDictionaryType(userId, foodId, type) != null
        if (exists) return

        dictionaryRepository.save(
            UserFoodDictionary(
                user = userRepository.getReferenceById(userId),
                food = foodRepository.getReferenceById(foodId),
                dictionaryType = type,
            ),
        )
    }
}

private val SAFE_STATES = setOf(SymptomState.COMFORTABLE, SymptomState.GOOD)

fun SymptomState.isSafe() = this in SAFE_STATES
