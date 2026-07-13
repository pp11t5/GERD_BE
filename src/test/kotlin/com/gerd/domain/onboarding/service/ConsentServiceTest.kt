package com.gerd.domain.onboarding.service

import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.entity.Term
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.TermRepository
import com.gerd.domain.onboarding.repository.UserConsentRepository
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ConsentServiceTest {

    @Mock
    private lateinit var userConsentRepository: UserConsentRepository

    @Mock
    private lateinit var termRepository: TermRepository

    @Mock
    private lateinit var userNotificationSettingRepository: UserNotificationSettingRepository

    @InjectMocks
    private lateinit var consentService: ConsentService

    private val effectiveDate = LocalDate.of(2026, 1, 1)

    private fun term(id: Long, code: String, required: Boolean = true) = Term(
        id = id, code = code, version = "1.0",
        title = "${code} 약관", content = "내용",
        required = required, effectiveDate = effectiveDate,
    )

    private val tosTerm = term(1L, "tos")
    private val privacyTerm = term(2L, "privacy")
    private val healthTerm = term(3L, "health_sensitive")
    private val marketingTerm = term(4L, "marketing", required = false)
    private val allTerms = listOf(tosTerm, privacyTerm, healthTerm, marketingTerm)

    private fun allConsentRequest(marketing: Boolean = false) = ConsentRequestDTO(
        consents = listOf(
            ConsentRequestDTO.ConsentItem(1L, true),
            ConsentRequestDTO.ConsentItem(2L, true),
            ConsentRequestDTO.ConsentItem(3L, true),
            ConsentRequestDTO.ConsentItem(4L, marketing),
        ),
    )

    private fun existingSetting() = UserNotificationSetting(user = UserFixture.user())

    @Nested
    inner class `submitConsent` {

        @Nested
        inner class `성공` {

            @Test
            fun `마케팅 동의 시 알림 설정을 모두 true로 업데이트한다`() {
                val setting = existingSetting()
                whenever(termRepository.findLatestAll()).thenReturn(allTerms)
                whenever(userConsentRepository.findByIdUserId(1L)).thenReturn(emptyList())
                whenever(userNotificationSettingRepository.findById(1L)).thenReturn(Optional.of(setting))

                consentService.submitConsent(1L, allConsentRequest(marketing = true))

                assertThat(setting.postMealNotificationEnabled).isTrue()
                assertThat(setting.weeklyReportEnabled).isTrue()
            }

            @Test
            fun `마케팅 미동의 시 알림 설정을 모두 false로 업데이트한다`() {
                val setting = existingSetting()
                whenever(termRepository.findLatestAll()).thenReturn(allTerms)
                whenever(userConsentRepository.findByIdUserId(1L)).thenReturn(emptyList())
                whenever(userNotificationSettingRepository.findById(1L)).thenReturn(Optional.of(setting))

                consentService.submitConsent(1L, allConsentRequest(marketing = false))

                assertThat(setting.postMealNotificationEnabled).isFalse()
                assertThat(setting.weeklyReportEnabled).isFalse()
            }

            @Test
            fun `알림 설정이 없어도 약관 저장은 정상 수행된다`() {
                whenever(termRepository.findLatestAll()).thenReturn(allTerms)
                whenever(userConsentRepository.findByIdUserId(1L)).thenReturn(emptyList())
                whenever(userNotificationSettingRepository.findById(1L)).thenReturn(Optional.empty())

                consentService.submitConsent(1L, allConsentRequest())

                val captor = argumentCaptor<List<UserConsent>>()
                verify(userConsentRepository).saveAll(captor.capture())
                assertThat(captor.firstValue).hasSize(4)
            }

            @Test
            fun `기존 동의가 있으면 해당 행을 갱신한다`() {
                val existing = UserConsent(UserConsentId(1L, 4L), marketingTerm, agreed = true, agreedAt = LocalDateTime.now())
                whenever(termRepository.findLatestAll()).thenReturn(allTerms)
                whenever(userConsentRepository.findByIdUserId(1L)).thenReturn(listOf(existing))
                whenever(userNotificationSettingRepository.findById(1L)).thenReturn(Optional.of(existingSetting()))

                consentService.submitConsent(1L, allConsentRequest(marketing = false))

                assertThat(existing.agreed).isFalse()
                verify(userConsentRepository).saveAll(any<List<UserConsent>>())
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `필수 약관에 미동의하면 REQUIRED_CONSENT_NOT_AGREED 예외가 발생한다`() {
                val request = ConsentRequestDTO(
                    consents = listOf(
                        ConsentRequestDTO.ConsentItem(1L, true),
                        ConsentRequestDTO.ConsentItem(2L, false),
                        ConsentRequestDTO.ConsentItem(3L, true),
                        ConsentRequestDTO.ConsentItem(4L, false),
                    ),
                )
                whenever(termRepository.findLatestAll()).thenReturn(allTerms)

                assertThatThrownBy { consentService.submitConsent(1L, request) }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(OnboardingErrorCode.REQUIRED_CONSENT_NOT_AGREED)

                verify(userConsentRepository, never()).saveAll(any<List<UserConsent>>())
            }
        }
    }

    @Nested
    inner class `isMarketingAgreed` {

        @Test
        fun `마케팅 동의가 true면 true를 반환한다`() {
            val consent = UserConsent(UserConsentId(1L, 4L), marketingTerm, agreed = true, agreedAt = LocalDateTime.now())
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")).thenReturn(consent)

            assertThat(consentService.isMarketingAgreed(1L)).isTrue()
        }

        @Test
        fun `마케팅 동의가 false면 false를 반환한다`() {
            val consent = UserConsent(UserConsentId(1L, 4L), marketingTerm, agreed = false, agreedAt = LocalDateTime.now())
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")).thenReturn(consent)

            assertThat(consentService.isMarketingAgreed(1L)).isFalse()
        }

        @Test
        fun `마케팅 동의 내역이 없으면 false를 반환한다`() {
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")).thenReturn(null)

            assertThat(consentService.isMarketingAgreed(1L)).isFalse()
        }
    }

    @Nested
    inner class `toggleMarketing` {

        @Test
        fun `마케팅 동의를 off에서 on으로 전환하고 알림 설정을 활성화한다`() {
            val setting = existingSetting()
            val consent = UserConsent(UserConsentId(1L, 4L), marketingTerm, agreed = false, agreedAt = LocalDateTime.now())
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")).thenReturn(consent)
            whenever(userNotificationSettingRepository.findById(1L)).thenReturn(Optional.of(setting))

            val result = consentService.toggleMarketing(1L)

            assertThat(result).isTrue()
            assertThat(consent.agreed).isTrue()
            assertThat(setting.postMealNotificationEnabled).isTrue()
            assertThat(setting.weeklyReportEnabled).isTrue()
        }

        @Test
        fun `마케팅 동의를 on에서 off로 전환하고 알림 설정을 비활성화한다`() {
            val setting = existingSetting()
            val consent = UserConsent(UserConsentId(1L, 4L), marketingTerm, agreed = true, agreedAt = LocalDateTime.now())
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")).thenReturn(consent)
            whenever(userNotificationSettingRepository.findById(1L)).thenReturn(Optional.of(setting))

            val result = consentService.toggleMarketing(1L)

            assertThat(result).isFalse()
            assertThat(consent.agreed).isFalse()
            assertThat(setting.postMealNotificationEnabled).isFalse()
        }

        @Test
        fun `마케팅 동의 내역이 없으면 MARKETING_CONSENT_NOT_FOUND 예외가 발생한다`() {
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")).thenReturn(null)

            assertThatThrownBy { consentService.toggleMarketing(1L) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(OnboardingErrorCode.MARKETING_CONSENT_NOT_FOUND)
        }
    }
}
