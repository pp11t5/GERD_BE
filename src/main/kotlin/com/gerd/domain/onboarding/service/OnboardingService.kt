package com.gerd.domain.onboarding.service

import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.food.repository.AllergenRepository
import com.gerd.domain.food.repository.TriggerLabelRepository
import com.gerd.domain.onboarding.dto.OnboardingRequestDTO
import com.gerd.domain.onboarding.dto.OnboardingStatusResponseDTO
import com.gerd.domain.onboarding.entity.UserAllergen
import com.gerd.domain.onboarding.entity.UserMedication
import com.gerd.domain.onboarding.entity.UserProfile
import com.gerd.domain.onboarding.entity.UserSymptom
import com.gerd.domain.onboarding.entity.UserTrigger
import com.gerd.domain.onboarding.entity.id.UserSymptomId
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserMedicationRepository
import com.gerd.domain.onboarding.repository.UserProfileRepository
import com.gerd.domain.onboarding.repository.UserSymptomRepository
import com.gerd.domain.onboarding.repository.UserTriggerRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 온보딩 조회·제출 서비스
 *
 * - 온보딩 완료 여부 조회 (user_profiles row 존재)
 * - 4단계(증상/트리거/알레르기/복용약) 일괄 제출 (단일 트랜잭션)
 */
@Service
@Transactional(readOnly = true)
class OnboardingService(
    private val userProfileRepository: UserProfileRepository,
    private val userSymptomRepository: UserSymptomRepository,
    private val userTriggerRepository: UserTriggerRepository,
    private val userAllergenRepository: UserAllergenRepository,
    private val userMedicationRepository: UserMedicationRepository,
    private val triggerLabelRepository: TriggerLabelRepository,
    private val allergenRepository: AllergenRepository,
) {

    fun getStatus(userId: Long): OnboardingStatusResponseDTO =
        OnboardingStatusResponseDTO(onboarded = userProfileRepository.existsById(userId))

    @Transactional
    fun submit(userId: Long, request: OnboardingRequestDTO) {
        // row 존재 = 온보딩 완료 → 재제출 불가
        if (userProfileRepository.existsById(userId)) {
            throw GeneralException(OnboardingErrorCode.ALREADY_ONBOARDED)
        }

        // code → master 엔티티 resolve를 먼저 끝내 시드 누락 시 프로필 생성 전에 차단
        val triggerLabels = resolveTriggers(request.triggers)
        val allergens = resolveAllergens(request.allergens)

        val profile = userProfileRepository.save(
            UserProfile(
                userId = userId,
                customTriggerText = request.customTriggerText,
                onboardedAt = LocalDateTime.now(),
            ),
        )

        if (request.symptoms.isNotEmpty()) {
            userSymptomRepository.saveAll(
                request.symptoms.distinct().map { symptom ->
                    UserSymptom(userProfile = profile, id = UserSymptomId(symptomCode = symptom.code))
                },
            )
        }
        if (triggerLabels.isNotEmpty()) {
            userTriggerRepository.saveAll(
                triggerLabels.map { label -> UserTrigger(userProfile = profile, triggerLabel = label) },
            )
        }
        if (allergens.isNotEmpty()) {
            userAllergenRepository.saveAll(
                allergens.map { allergen -> UserAllergen(userProfile = profile, allergen = allergen) },
            )
        }
        if (request.medications.isNotEmpty()) {
            userMedicationRepository.saveAll(
                request.medications.map { name -> UserMedication(userProfile = profile, name = name) },
            )
        }
    }

    private fun resolveTriggers(codes: List<TriggerCode>): List<TriggerLabel> {
        if (codes.isEmpty()) return emptyList()
        val distinctCodes = codes.map { it.code }.distinct()
        val labels = triggerLabelRepository.findByCodeIn(distinctCodes)
        if (labels.size < distinctCodes.size) {
            throw GeneralException(OnboardingErrorCode.INVALID_TRIGGER)
        }
        return labels
    }

    private fun resolveAllergens(codes: List<AllergenCode>): List<Allergen> {
        if (codes.isEmpty()) return emptyList()
        val distinctCodes = codes.map { it.code }.distinct()
        val allergens = allergenRepository.findByCodeIn(distinctCodes)
        if (allergens.size < distinctCodes.size) {
            throw GeneralException(OnboardingErrorCode.INVALID_ALLERGEN)
        }
        return allergens
    }
}
