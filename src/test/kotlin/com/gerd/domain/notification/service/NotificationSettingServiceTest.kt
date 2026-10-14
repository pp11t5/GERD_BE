package com.gerd.domain.notification.service

import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.exception.NotificationErrorCode
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.domain.onboarding.entity.Term
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.repository.UserConsentRepository
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
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class NotificationSettingServiceTest {

    @Mock
    private lateinit var userNotificationSettingRepository: UserNotificationSettingRepository

    @Mock
    private lateinit var userConsentRepository: UserConsentRepository

    @InjectMocks
    private lateinit var notificationSettingService: NotificationSettingService

    private val userId = 1L

    private val marketingTerm = Term(
        id = 4L, code = "marketing", version = "1.0",
        title = "marketing 약관", content = "내용",
        required = false, effectiveDate = LocalDate.of(2026, 1, 1),
    )

    private fun consent(agreed: Boolean) =
        UserConsent(UserConsentId(userId, 4L), marketingTerm, agreed = agreed, agreedAt = LocalDateTime.now())

    @Nested
    inner class `getNotificationSetting` {

        @Test
        fun `마케팅 동의가 true면 marketingEnabled가 true로 조회된다`() {
            whenever(userNotificationSettingRepository.findById(userId))
                .thenReturn(Optional.of(UserNotificationSetting(user = UserFixture.user())))
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(userId, "marketing")).thenReturn(consent(true))

            val result = notificationSettingService.getNotificationSetting(userId)

            assertThat(result.marketingEnabled).isTrue()
        }

        @Test
        fun `마케팅 동의가 false면 marketingEnabled가 false로 조회된다`() {
            whenever(userNotificationSettingRepository.findById(userId))
                .thenReturn(Optional.of(UserNotificationSetting(user = UserFixture.user())))
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(userId, "marketing")).thenReturn(consent(false))

            val result = notificationSettingService.getNotificationSetting(userId)

            assertThat(result.marketingEnabled).isFalse()
        }

        @Test
        fun `마케팅 동의 내역이 없으면 marketingEnabled가 false로 조회된다`() {
            whenever(userNotificationSettingRepository.findById(userId))
                .thenReturn(Optional.of(UserNotificationSetting(user = UserFixture.user())))
            whenever(userConsentRepository.findLatestByUserIdAndTermCode(userId, "marketing")).thenReturn(null)

            val result = notificationSettingService.getNotificationSetting(userId)

            assertThat(result.marketingEnabled).isFalse()
        }

        @Test
        fun `알림 설정이 없으면 NOTIFICATION_SETTING_NOT_FOUND 예외가 발생한다`() {
            whenever(userNotificationSettingRepository.findById(userId)).thenReturn(Optional.empty())

            assertThatThrownBy { notificationSettingService.getNotificationSetting(userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.NOTIFICATION_SETTING_NOT_FOUND)
        }
    }
}
