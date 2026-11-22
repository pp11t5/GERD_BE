package com.gerd.domain.onboarding.service

import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.entity.Term
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.TermRepository
import com.gerd.domain.onboarding.repository.UserConsentRepository
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
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ConsentServiceTest {

    @Mock
    private lateinit var userConsentRepository: UserConsentRepository

    @Mock
    private lateinit var termRepository: TermRepository

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

    @Nested
    inner class `submitConsent` {

        @Nested
        inner class `성공` {

            @Test
            fun `4개 약관을 신규 저장한다`() {
                whenever(termRepository.findLatestAll()).thenReturn(allTerms)
                whenever(userConsentRepository.findByIdUserId(1L)).thenReturn(emptyList())

                consentService.submitConsent(1L, allConsentRequest())

                val captor = argumentCaptor<List<UserConsent>>()
                verify(userConsentRepository).saveAll(captor.capture())
                assertThat(captor.firstValue).hasSize(4)
                assertThat(captor.firstValue.first { it.id.termId == 4L }.agreed).isFalse()
            }

            @Test
            fun `기존 동의가 있으면 해당 행을 갱신한다`() {
                val existing = UserConsent(UserConsentId(1L, 4L), marketingTerm, agreed = true, agreedAt = LocalDateTime.now())
                whenever(termRepository.findLatestAll()).thenReturn(allTerms)
                whenever(userConsentRepository.findByIdUserId(1L)).thenReturn(listOf(existing))

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
        fun `마케팅 동의를 off에서 on으로 전환하고 변경된 상태를 반환한다`() {
            val consent = UserConsent(UserConsentId(1L, 4L), marketingTerm, agreed = false, agreedAt = LocalDateTime.now())
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")).thenReturn(consent)

            val result = consentService.toggleMarketing(1L)

            assertThat(result).isTrue()
            assertThat(consent.agreed).isTrue()
        }

        @Test
        fun `마케팅 동의를 on에서 off로 전환하고 변경된 상태를 반환한다`() {
            val consent = UserConsent(UserConsentId(1L, 4L), marketingTerm, agreed = true, agreedAt = LocalDateTime.now())
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")).thenReturn(consent)

            val result = consentService.toggleMarketing(1L)

            assertThat(result).isFalse()
            assertThat(consent.agreed).isFalse()
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
