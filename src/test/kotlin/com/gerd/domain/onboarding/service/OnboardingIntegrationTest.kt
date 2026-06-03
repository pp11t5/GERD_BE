package com.gerd.domain.onboarding.service

import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.food.repository.AllergenRepository
import com.gerd.domain.food.repository.TriggerLabelRepository
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.dto.OnboardingRequestDTO
import com.gerd.domain.onboarding.entity.enums.SymptomCode
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserConsentRepository
import com.gerd.domain.onboarding.repository.UserMedicationRepository
import com.gerd.domain.onboarding.repository.UserProfileRepository
import com.gerd.domain.onboarding.repository.UserSymptomRepository
import com.gerd.domain.onboarding.repository.UserTriggerRepository
import com.gerd.global.apiPayload.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class OnboardingIntegrationTest @Autowired constructor(
    private val onboardingService: OnboardingService,
    private val consentService: ConsentService,
    private val userProfileRepository: UserProfileRepository,
    private val userConsentRepository: UserConsentRepository,
    private val userSymptomRepository: UserSymptomRepository,
    private val userTriggerRepository: UserTriggerRepository,
    private val userAllergenRepository: UserAllergenRepository,
    private val userMedicationRepository: UserMedicationRepository,
    private val triggerLabelRepository: TriggerLabelRepository,
    private val allergenRepository: AllergenRepository,
) {

    @AfterEach
    fun tearDown() {
        // 자식 → 부모 → 마스터 순으로 FK 제약을 지키며 정리
        userSymptomRepository.deleteAll()
        userTriggerRepository.deleteAll()
        userAllergenRepository.deleteAll()
        userMedicationRepository.deleteAll()
        userProfileRepository.deleteAll()
        userConsentRepository.deleteAll()
        triggerLabelRepository.deleteAll()
        allergenRepository.deleteAll()
    }

    private fun seedMasters() {
        triggerLabelRepository.save(TriggerLabel(code = "caffeine", displayName = "커피·카페인"))
        allergenRepository.save(Allergen(code = "milk", displayName = "우유·유제품"))
    }

    @Nested
    inner class `동의-제출-조회 흐름` {

        @Test
        fun `동의 후 온보딩을 제출하면 완료 상태가 되고 자식이 모두 저장된다`() {
            val userId = 1L
            seedMasters()

            consentService.submitConsent(
                userId,
                ConsentRequestDTO(tos = true, privacy = true, healthSensitive = true, marketing = false),
            )
            assertThat(onboardingService.getStatus(userId).onboarded).isFalse()

            onboardingService.submit(
                userId,
                OnboardingRequestDTO(
                    symptoms = listOf(SymptomCode.HEARTBURN_REFLUX, SymptomCode.THROAT_GLOBUS),
                    triggers = listOf(TriggerCode.CAFFEINE),
                    allergens = listOf(AllergenCode.MILK),
                    medications = listOf("PPI", "제산제"),
                    customTriggerText = "오렌지주스",
                ),
            )

            assertThat(onboardingService.getStatus(userId).onboarded).isTrue()
            assertThat(userProfileRepository.findById(userId)).isPresent
            assertThat(userConsentRepository.findByIdUserId(userId)).hasSize(4)
            assertThat(userSymptomRepository.count()).isEqualTo(2)
            assertThat(userTriggerRepository.count()).isEqualTo(1)
            assertThat(userAllergenRepository.count()).isEqualTo(1)
            assertThat(userMedicationRepository.count()).isEqualTo(2)
        }

        @Test
        fun `이미 온보딩한 사용자가 재제출하면 409 예외가 발생한다`() {
            val userId = 2L
            seedMasters()
            onboardingService.submit(userId, OnboardingRequestDTO(triggers = listOf(TriggerCode.CAFFEINE)))

            assertThatThrownBy { onboardingService.submit(userId, OnboardingRequestDTO()) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(OnboardingErrorCode.ALREADY_ONBOARDED)
        }
    }

    @Nested
    inner class `단일 트랜잭션 원자성` {

        @Test
        fun `시드되지 않은 trigger code면 예외가 발생하고 프로필이 생성되지 않는다`() {
            val userId = 3L
            // caffeine만 시드, SPICY는 미시드
            triggerLabelRepository.save(TriggerLabel(code = "caffeine", displayName = "커피·카페인"))

            assertThatThrownBy {
                onboardingService.submit(
                    userId,
                    OnboardingRequestDTO(triggers = listOf(TriggerCode.CAFFEINE, TriggerCode.SPICY)),
                )
            }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(OnboardingErrorCode.INVALID_TRIGGER)

            assertThat(userProfileRepository.existsById(userId)).isFalse()
        }
    }
}
