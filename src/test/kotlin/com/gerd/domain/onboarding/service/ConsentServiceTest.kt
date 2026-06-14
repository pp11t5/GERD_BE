package com.gerd.domain.onboarding.service

import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.UserConsentRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.UserFixture
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
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ConsentServiceTest {

    @Mock
    private lateinit var userConsentRepository: UserConsentRepository

    @Mock
    private lateinit var userNotificationSettingRepository: UserNotificationSettingRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var consentService: ConsentService

    @Nested
    inner class `submitConsent` {

        @Nested
        inner class `성공` {

            @Test
            fun `기존 동의가 없으면 4개 type을 신규 저장한다`() {
                whenever(userConsentRepository.findByIdUserId(1L)).thenReturn(emptyList())
                whenever(userNotificationSettingRepository.findById(1L))
                    .thenReturn(Optional.of(UserNotificationSetting(user = UserFixture.user())))
                val request = ConsentRequestDTO(tos = true, privacy = true, healthSensitive = true, marketing = false)

                consentService.submitConsent(1L, request)

                val captor = argumentCaptor<List<UserConsent>>()
                verify(userConsentRepository).saveAll(captor.capture())
                val saved = captor.firstValue
                assertThat(saved).hasSize(4)
                assertThat(saved.map { it.id.consentType })
                    .containsExactlyInAnyOrder("tos", "privacy", "health_sensitive", "marketing")
                assertThat(saved.first { it.id.consentType == "marketing" }.agreed).isFalse()
            }

            @Test
            fun `기존 동의가 있으면 해당 행을 갱신한다`() {
                val existing = UserConsent(UserConsentId(1L, "marketing"), agreed = true, agreedAt = LocalDateTime.now())
                whenever(userConsentRepository.findByIdUserId(1L)).thenReturn(listOf(existing))
                whenever(userNotificationSettingRepository.findById(1L))
                    .thenReturn(Optional.of(UserNotificationSetting(user = UserFixture.user())))
                val request = ConsentRequestDTO(tos = true, privacy = true, healthSensitive = true, marketing = false)

                consentService.submitConsent(1L, request)

                // 기존 marketing 행이 false로 갱신되어 재저장 대상에 포함
                assertThat(existing.agreed).isFalse()
                verify(userConsentRepository).saveAll(any<List<UserConsent>>())
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `필수 약관에 미동의하면 REQUIRED_CONSENT_NOT_AGREED 예외가 발생한다`() {
                val request = ConsentRequestDTO(tos = true, privacy = false, healthSensitive = true, marketing = true)

                assertThatThrownBy { consentService.submitConsent(1L, request) }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(OnboardingErrorCode.REQUIRED_CONSENT_NOT_AGREED)

                verify(userConsentRepository, never()).saveAll(any<List<UserConsent>>())
            }
        }
    }
}
