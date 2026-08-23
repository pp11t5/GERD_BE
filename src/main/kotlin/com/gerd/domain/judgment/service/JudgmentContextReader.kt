package com.gerd.domain.judgment.service

import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodAllergenRepository
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.repository.FoodSubstituteRepository
import com.gerd.domain.food.repository.FoodTriggerRepository
import com.gerd.domain.food.service.FoodAccessPolicy
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.judgment.dto.JudgmentContext
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.judgment.dto.UserContext
import com.gerd.domain.judgment.dto.SubstituteCandidateDTO
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserSymptomRepository
import com.gerd.domain.onboarding.repository.UserTriggerRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomMealPatternRow
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * 판정에 필요한 음식·사용자 컨텍스트를 짧은 트랜잭션에서 일괄 로딩
 *
 * 오케스트레이터가 LLM 호출(수 초) 동안 DB 커넥션을 점유하지 않도록
 * DB 읽기는 전부 이 컴포넌트의 readOnly 트랜잭션 안에서 끝낸다
 */
@Component
@Transactional(readOnly = true)
class JudgmentContextReader(
    private val foodRepository: FoodRepository,
    private val foodTriggerRepository: FoodTriggerRepository,
    private val foodAllergenRepository: FoodAllergenRepository,
    private val foodSubstituteRepository: FoodSubstituteRepository,
    private val foodCategoryReader: FoodCategoryReader,
    private val userTriggerRepository: UserTriggerRepository,
    private val userAllergenRepository: UserAllergenRepository,
    private val userSymptomRepository: UserSymptomRepository,
    private val symptomRepository: SymptomRepository,
    private val mealRecordRepository: MealRecordRepository,
) {

    fun load(foodExternalId: String, userId: Long): JudgmentContext {
        // 형식이 잘못된 UUID는 존재할 수 없는 음식과 동일하게 취급(열거 단서 차단)
        val externalId = parseUuid(foodExternalId) ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        val food = foodRepository.findByExternalId(externalId)
            ?.takeIf { FoodAccessPolicy.isVisibleTo(it, userId) }
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)

        val foodId = requireNotNull(food.id) { "영속 음식은 id를 가진다" }

        // ⓪ 출처 게이트 대상(유저 입력 음식)은 LLM을 타지 않으므로 부가 컨텍스트 조회를 생략한다
        // 단, 증상 기록은 USER 음식도 무조건 조회한다
        if (food.source == FoodSource.USER) {
            return JudgmentContext(
                food = food,
                category = null,
                foodTriggers = emptyList(),
                foodAllergens = emptyList(),
                userTriggers = emptyList(),
                userAllergens = emptyList(),
                symptomCodes = emptyList(),
                stateRecords = loadStateRecords(userId, foodId),
            )
        }

        val category = foodCategoryReader.loadPrimaryByFoodIds(listOf(foodId))[foodId]
        return JudgmentContext(
            food = food,
            category = category,
            foodTriggers = foodTriggerRepository.findTriggerLabelsByFoodId(foodId)
                .map { TagDTO(it.code, it.displayName) },
            foodAllergens = foodAllergenRepository.findAllergensByFoodId(foodId)
                .map { TagDTO(it.code, it.displayName) },
            userTriggers = userTriggerRepository.findTriggerLabelsByUserId(userId)
                .map { TagDTO(it.code, it.displayName) },
            userAllergens = userAllergenRepository.findAllergensByUserId(userId)
                .map { TagDTO(it.code, it.displayName) },
            symptomCodes = userSymptomRepository.findByIdUserId(userId).map { it.id.symptomCode },
            history = loadHistory(userId, food.name, category),
            stateRecords = loadStateRecords(userId, foodId),
        )
    }

    fun loadUserContext(userId: Long): UserContext = UserContext(
        userTriggers = userTriggerRepository.findTriggerLabelsByUserId(userId).map { TagDTO(it.code, it.displayName) },
        userAllergens = userAllergenRepository.findAllergensByUserId(userId).map { TagDTO(it.code, it.displayName) },
        symptomCodes = userSymptomRepository.findByIdUserId(userId).map { it.id.symptomCode },
    )

    fun loadHistoryForText(userId: Long, foodName: String): LlmInputSnapshotDTO.HistorySnapshotDTO =
        loadHistory(userId, foodName, category = null)

    // LLM 호출 이후 별도 짧은 트랜잭션으로 조회 — 캐시 loader 안의 커넥션 점유 시간을 최소화.
    // 후보의 트리거·알레르겐 코드를 함께 실어 서비스가 사용자별 안전 필터를 적용할 수 있게 한다
    fun loadSubstituteCandidates(foodId: Long): List<SubstituteCandidateDTO> {
        val pairs = foodSubstituteRepository.findByFoodIdOrderBySortOrder(foodId)
        if (pairs.isEmpty()) return emptyList()

        val substituteIds = pairs.map { requireNotNull(it.substituteFood.id) { "영속 음식은 id를 가진다" } }
        val codesByFoodId = (
            foodTriggerRepository.findTagCodesByFoodIdIn(substituteIds) +
                foodAllergenRepository.findTagCodesByFoodIdIn(substituteIds)
            ).groupBy({ it.foodId }, { it.code })

        return pairs.map {
            val substituteId = requireNotNull(it.substituteFood.id) { "영속 음식은 id를 가진다" }
            SubstituteCandidateDTO(
                foodExternalId = requireNotNull(it.substituteFood.externalId) { "영속 음식은 externalId를 가진다" }.toString(),
                name = it.substituteFood.name,
                tagCodes = codesByFoodId[substituteId]?.toSet() ?: emptySet(),
            )
        }
    }

    private fun parseUuid(value: String): UUID? =
        runCatching { UUID.fromString(value.trim()) }.getOrNull()

    private fun loadHistory(userId: Long, foodName: String, category: String?): LlmInputSnapshotDTO.HistorySnapshotDTO {
        val rows = symptomRepository.findLinkedRows(userId, LocalDateTime.now().minusDays(HISTORY_WINDOW_DAYS.toLong()))
            .filter { row -> row.foodName == foodName || (category != null && row.category == category) }
        val distinctRows = rows.distinctBy { it.symptomInternalId }
        return LlmInputSnapshotDTO.HistorySnapshotDTO(
            similarFoodRecords = buildSimilarFoodRecords(rows),
            comfortCount = distinctRows.count { it.symptomState.isComfort() },
            discomfortCount = distinctRows.count { it.symptomState.isDiscomfort() },
        )
    }

    private fun buildSimilarFoodRecords(rows: List<SymptomMealPatternRow>): List<LlmInputSnapshotDTO.SimilarFoodRecordDTO> =
        rows
            .distinctBy { "${it.symptomInternalId}:${it.foodName}" }
            .groupBy { it.foodName to it.symptomState.historyState() }
            .map { (key, records) ->
                LlmInputSnapshotDTO.SimilarFoodRecordDTO(
                    food = key.first,
                    state = key.second,
                    count = records.size,
                )
            }
            .sortedWith(
                compareByDescending<LlmInputSnapshotDTO.SimilarFoodRecordDTO> { it.count }
                    .thenBy { it.food },
            )
            .take(MAX_HISTORY_RECORDS)

    private fun loadStateRecords(userId: Long, foodId: Long): JudgmentResponseDTO.StateRecordsDTO {
        val symptoms = symptomRepository.findLinkedSymptomsByUserIdAndFoodId(userId, foodId)
        if (symptoms.isEmpty()) return JudgmentResponseDTO.StateRecordsDTO(total = 0, records = emptyList())

        val eatenAtByMealRecordId = mealRecordRepository
            .findAllById(symptoms.mapNotNull { it.mealRecordId }.distinct())
            .associateBy({ requireNotNull(it.id) }) { it.eatenAt }

        val records = symptoms
            .filter { it.mealRecordId != null && eatenAtByMealRecordId.containsKey(it.mealRecordId) }
            .take(STATE_RECORDS_LIMIT)
            .map { symptom ->
                val eatenAt = eatenAtByMealRecordId[symptom.mealRecordId]!!
                val afterMinutes = ChronoUnit.MINUTES.between(eatenAt, symptom.occurredAt).toInt()
                JudgmentResponseDTO.JudgmentStateRecordDTO(
                    label = symptom.symptomState.toLabel(),
                    date = symptom.occurredAt.toLocalDate().toString(),
                    timing = "식후 ${afterMinutes}분",
                )
            }

        return JudgmentResponseDTO.StateRecordsDTO(total = symptoms.size, records = records)
    }

    private fun SymptomState.toLabel(): String = when (this) {
        SymptomState.COMFORTABLE -> "편안해요"
        SymptomState.GOOD -> "양호해요"
        SymptomState.NORMAL -> "보통이에요"
        SymptomState.UNCOMFORTABLE -> "불편해요"
        SymptomState.SEVERE -> "심각해요"
    }

    private fun SymptomState.isComfort(): Boolean =
        this == SymptomState.COMFORTABLE || this == SymptomState.GOOD

    private fun SymptomState.isDiscomfort(): Boolean =
        this == SymptomState.UNCOMFORTABLE || this == SymptomState.SEVERE

    private fun SymptomState.historyState(): String =
        when {
            isComfort() -> "comfort"
            isDiscomfort() -> "discomfort"
            else -> "neutral"
        }

    companion object {
        private const val HISTORY_WINDOW_DAYS = 14
        private const val MAX_HISTORY_RECORDS = 5
        private const val STATE_RECORDS_LIMIT = 3
    }
}
