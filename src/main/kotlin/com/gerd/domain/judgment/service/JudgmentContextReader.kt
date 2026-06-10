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
import com.gerd.domain.judgment.dto.JudgmentResponseDTO.SubstituteDTO
import com.gerd.domain.judgment.dto.LlmInputSnapshotDTO.TagDTO
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserMedicationRepository
import com.gerd.domain.onboarding.repository.UserSymptomRepository
import com.gerd.domain.onboarding.repository.UserTriggerRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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
    private val userMedicationRepository: UserMedicationRepository,
    private val userSymptomRepository: UserSymptomRepository,
) {

    fun load(foodExternalId: String, userId: Long): JudgmentContext {
        // 형식이 잘못된 UUID는 존재할 수 없는 음식과 동일하게 취급(열거 단서 차단)
        val externalId = parseUuid(foodExternalId) ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)
        val food = foodRepository.findByExternalId(externalId)
            ?.takeIf { FoodAccessPolicy.isVisibleTo(it, userId) }
            ?: throw GeneralException(FoodErrorCode.FOOD_NOT_FOUND)

        // ⓪ 출처 게이트 대상(유저 입력 음식)은 LLM을 타지 않으므로 부가 컨텍스트 조회를 생략한다
        if (food.source == FoodSource.USER) {
            return JudgmentContext(
                food = food,
                category = null,
                foodTriggers = emptyList(),
                foodAllergens = emptyList(),
                userTriggers = emptyList(),
                userAllergens = emptyList(),
                medications = emptyList(),
                symptomCodes = emptyList(),
            )
        }

        val foodId = requireNotNull(food.id) { "영속 음식은 id를 가진다" }
        return JudgmentContext(
            food = food,
            category = foodCategoryReader.loadPrimaryByFoodIds(listOf(foodId))[foodId],
            foodTriggers = foodTriggerRepository.findTriggerLabelsByFoodId(foodId)
                .map { TagDTO(it.code, it.displayName) },
            foodAllergens = foodAllergenRepository.findAllergensByFoodId(foodId)
                .map { TagDTO(it.code, it.displayName) },
            userTriggers = userTriggerRepository.findTriggerLabelsByUserId(userId)
                .map { TagDTO(it.code, it.displayName) },
            userAllergens = userAllergenRepository.findAllergensByUserId(userId)
                .map { TagDTO(it.code, it.displayName) },
            medications = userMedicationRepository.findByUserProfileUserId(userId).map { it.name },
            symptomCodes = userSymptomRepository.findByIdUserId(userId).map { it.id.symptomCode },
        )
    }

    // LLM 호출 이후 별도 짧은 트랜잭션으로 조회 — 캐시 loader 안의 커넥션 점유 시간을 최소화
    fun loadSubstitutes(foodId: Long): List<SubstituteDTO> =
        foodSubstituteRepository.findByFoodIdOrderBySortOrder(foodId).map {
            SubstituteDTO(
                foodExternalId = requireNotNull(it.substituteFood.externalId) { "영속 음식은 externalId를 가진다" }.toString(),
                name = it.substituteFood.name,
            )
        }

    private fun parseUuid(value: String): UUID? =
        try {
            UUID.fromString(value.trim())
        } catch (e: IllegalArgumentException) {
            null
        }
}
