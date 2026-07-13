package com.gerd.domain.onboarding.service

import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.food.repository.AllergenRepository
import com.gerd.domain.food.repository.TriggerLabelRepository
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.dto.OnboardingRequestDTO
import com.gerd.domain.onboarding.entity.Term
import com.gerd.domain.onboarding.entity.enums.SymptomCode
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.TermRepository
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
import java.time.LocalDate

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
    private val termRepository: TermRepository,
    private val userRepository: UserRepository,
) {

    @AfterEach
    fun tearDown() {
        userSymptomRepository.deleteAll()
        userTriggerRepository.deleteAll()
        userAllergenRepository.deleteAll()
        userMedicationRepository.deleteAll()
        userProfileRepository.deleteAll()
        userConsentRepository.deleteAll()
        termRepository.deleteAll()
        triggerLabelRepository.deleteAll()
        allergenRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun seedMasters() {
        triggerLabelRepository.save(TriggerLabel(code = "caffeine", displayName = "커피·카페인"))
        allergenRepository.save(Allergen(code = "milk", displayName = "우유·유제품"))
    }

    private fun seedTerms(): Map<String, Long> {
        val effectiveDate = LocalDate.of(2026, 1, 1)
        return listOf(
            Term(code = "tos", version = "1.0", title = "서비스 이용약관", content = "내용", required = true, effectiveDate = effectiveDate),
            Term(code = "privacy", version = "1.0", title = "개인정보 수집·이용", content = "내용", required = true, effectiveDate = effectiveDate),
            Term(code = "health_sensitive", version = "1.0", title = "민감정보 수집", content = "내용", required = true, effectiveDate = effectiveDate),
            Term(code = "marketing", version = "1.0", title = "마케팅 동의", content = "내용", required = false, effectiveDate = effectiveDate),
        ).associate { it.code to termRepository.save(it).id }
    }

    private fun seedUser(email: String): Long =
        userRepository.save(User(email = email, nickname = email.substringBefore("@"), role = UserRole.USER)).id!!

    @Nested
    inner class `동의-제출-조회 흐름` {

        @Test
        fun `동의 후 온보딩을 제출하면 완료 상태가 되고 자식이 모두 저장된다`() {
            val userId = seedUser("flow@test.com")
            seedMasters()
            val termIds = seedTerms()

            consentService.submitConsent(
                userId,
                ConsentRequestDTO(
                    consents = listOf(
                        ConsentRequestDTO.ConsentItem(termIds["tos"]!!, true),
                        ConsentRequestDTO.ConsentItem(termIds["privacy"]!!, true),
                        ConsentRequestDTO.ConsentItem(termIds["health_sensitive"]!!, true),
                        ConsentRequestDTO.ConsentItem(termIds["marketing"]!!, false),
                    ),
                ),
            )
            assertThat(onboardingService.getStatus(userId).onboarded).isFalse()

            onboardingService.submit(
                userId,
                OnboardingRequestDTO(
                    symptoms = setOf(SymptomCode.HEARTBURN_REFLUX, SymptomCode.THROAT_GLOBUS),
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
            val userId = seedUser("resubmit@test.com")
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
            val userId = seedUser("partial@test.com")
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
