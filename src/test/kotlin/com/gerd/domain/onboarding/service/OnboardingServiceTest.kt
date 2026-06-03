package com.gerd.domain.onboarding.service

import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.domain.food.entity.enums.AllergenCode
import com.gerd.domain.food.entity.enums.TriggerCode
import com.gerd.domain.food.repository.AllergenRepository
import com.gerd.domain.food.repository.TriggerLabelRepository
import com.gerd.domain.onboarding.dto.OnboardingRequestDTO
import com.gerd.domain.onboarding.entity.UserMedication
import com.gerd.domain.onboarding.entity.UserProfile
import com.gerd.domain.onboarding.entity.UserSymptom
import com.gerd.domain.onboarding.entity.UserTrigger
import com.gerd.domain.onboarding.entity.enums.SymptomCode
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.UserAllergenRepository
import com.gerd.domain.onboarding.repository.UserMedicationRepository
import com.gerd.domain.onboarding.repository.UserProfileRepository
import com.gerd.domain.onboarding.repository.UserSymptomRepository
import com.gerd.domain.onboarding.repository.UserTriggerRepository
import com.gerd.global.apiPayload.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class OnboardingServiceTest {

    @Mock
    private lateinit var userProfileRepository: UserProfileRepository

    @Mock
    private lateinit var userSymptomRepository: UserSymptomRepository

    @Mock
    private lateinit var userTriggerRepository: UserTriggerRepository

    @Mock
    private lateinit var userAllergenRepository: UserAllergenRepository

    @Mock
    private lateinit var userMedicationRepository: UserMedicationRepository

    @Mock
    private lateinit var triggerLabelRepository: TriggerLabelRepository

    @Mock
    private lateinit var allergenRepository: AllergenRepository

    @InjectMocks
    private lateinit var onboardingService: OnboardingService

    @Nested
    inner class `getStatus` {

        @Test
        fun `프로필이 존재하면 onboarded true를 반환한다`() {
            whenever(userProfileRepository.existsById(1L)).thenReturn(true)

            val result = onboardingService.getStatus(1L)

            assertThat(result.onboarded).isTrue()
        }
    }

    @Nested
    inner class `submit` {

        @Nested
        inner class `성공` {

            @Test
            fun `code를 마스터로 resolve해 프로필과 자식을 저장한다`() {
                whenever(userProfileRepository.existsById(1L)).thenReturn(false)
                whenever(triggerLabelRepository.findByCodeIn(listOf("caffeine")))
                    .thenReturn(listOf(TriggerLabel(id = 10L, code = "caffeine", displayName = "커피·카페인")))
                whenever(allergenRepository.findByCodeIn(listOf("milk")))
                    .thenReturn(listOf(Allergen(id = 20L, code = "milk", displayName = "우유·유제품")))
                whenever(userProfileRepository.save(any<UserProfile>()))
                    .thenAnswer { it.arguments[0] as UserProfile }

                val request = OnboardingRequestDTO(
                    symptoms = listOf(SymptomCode.HEARTBURN_REFLUX),
                    triggers = listOf(TriggerCode.CAFFEINE),
                    allergens = listOf(AllergenCode.MILK),
                    medications = listOf("PPI"),
                    customTriggerText = "오렌지주스",
                )

                onboardingService.submit(1L, request)

                val profileCaptor = argumentCaptor<UserProfile>()
                verify(userProfileRepository).save(profileCaptor.capture())
                assertThat(profileCaptor.firstValue.userId).isEqualTo(1L)
                assertThat(profileCaptor.firstValue.customTriggerText).isEqualTo("오렌지주스")

                verify(userSymptomRepository).saveAll(any<List<UserSymptom>>())
                verify(userTriggerRepository).saveAll(any<List<UserTrigger>>())
                verify(userAllergenRepository).saveAll(any<List<com.gerd.domain.onboarding.entity.UserAllergen>>())
                verify(userMedicationRepository).saveAll(any<List<UserMedication>>())
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `이미 온보딩한 사용자면 ALREADY_ONBOARDED 예외가 발생한다`() {
                whenever(userProfileRepository.existsById(1L)).thenReturn(true)

                assertThatThrownBy { onboardingService.submit(1L, OnboardingRequestDTO()) }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(OnboardingErrorCode.ALREADY_ONBOARDED)

                verify(userProfileRepository, never()).save(any<UserProfile>())
            }

            @Test
            fun `시드되지 않은 trigger code면 INVALID_TRIGGER 예외가 발생하고 프로필을 만들지 않는다`() {
                whenever(userProfileRepository.existsById(1L)).thenReturn(false)
                // 요청 2개인데 마스터는 1개만 resolve → 시드 누락
                whenever(triggerLabelRepository.findByCodeIn(listOf("caffeine", "spicy")))
                    .thenReturn(listOf(TriggerLabel(id = 10L, code = "caffeine", displayName = "커피·카페인")))

                val request = OnboardingRequestDTO(triggers = listOf(TriggerCode.CAFFEINE, TriggerCode.SPICY))

                assertThatThrownBy { onboardingService.submit(1L, request) }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(OnboardingErrorCode.INVALID_TRIGGER)

                verify(userProfileRepository, never()).save(any<UserProfile>())
            }
        }
    }
}
