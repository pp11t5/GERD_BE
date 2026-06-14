package com.gerd.domain.notification.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.fcm.service.FcmPushSender
import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.PENDING
import com.gerd.domain.notification.entity.enums.NotificationSettingType
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.repository.NotificationPendingRepository
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostMealPendingSenderTest {

    @Mock private lateinit var notificationPendingRepository: NotificationPendingRepository
    @Mock private lateinit var userNotificationSettingRepository: UserNotificationSettingRepository
    @Mock private lateinit var userFcmTokenRepository: UserFcmTokenRepository
    @Mock private lateinit var fcmPushSender: FcmPushSender

    @InjectMocks private lateinit var sender: PostMealPendingSender

    private val userId = 1L
    private val pendingIds = listOf(10L)

    private fun enabledSetting() = mock<UserNotificationSetting>().also {
        whenever(it.isEnabled(NotificationSettingType.POST_MEAL)).thenReturn(true)
    }

    @Nested
    inner class `발송 스킵` {

        @Test
        fun `PENDING이 없으면 발송도 상태변경도 하지 않는다`() {
            whenever(notificationPendingRepository.findByIdInAndStatus(any(), eq(PENDING)))
                .thenReturn(emptyList())

            sender.sendForUser(userId, pendingIds)

            verify(fcmPushSender, never()).send(any(), any())
        }

        @Test
        fun `알림 설정이 비활성이면 발송하지 않고 CANCELLED 처리한다`() {
            val pending = mock<NotificationPending>()
            whenever(notificationPendingRepository.findByIdInAndStatus(any(), eq(PENDING)))
                .thenReturn(listOf(pending))
            val setting = mock<UserNotificationSetting>().also {
                whenever(it.isEnabled(NotificationSettingType.POST_MEAL)).thenReturn(false)
            }
            whenever(userNotificationSettingRepository.findById(userId)).thenReturn(Optional.of(setting))

            sender.sendForUser(userId, pendingIds)

            verify(pending).cancel()
            verify(fcmPushSender, never()).send(any(), any())
        }

        @Test
        fun `설정 row가 없으면(푸시 미동의) 발송하지 않고 CANCELLED 처리한다`() {
            val pending = mock<NotificationPending>()
            whenever(notificationPendingRepository.findByIdInAndStatus(any(), eq(PENDING)))
                .thenReturn(listOf(pending))
            whenever(userNotificationSettingRepository.findById(userId)).thenReturn(Optional.empty())

            sender.sendForUser(userId, pendingIds)

            verify(pending).cancel()
            verify(fcmPushSender, never()).send(any(), any())
        }

        @Test
        fun `FCM 토큰이 없으면 발송하지 않고 CANCELLED 처리한다`() {
            val pending = mock<NotificationPending>()
            whenever(notificationPendingRepository.findByIdInAndStatus(any(), eq(PENDING)))
                .thenReturn(listOf(pending))
            val setting = enabledSetting()
            whenever(userNotificationSettingRepository.findById(userId)).thenReturn(Optional.of(setting))
            whenever(userFcmTokenRepository.findById(userId)).thenReturn(Optional.empty())

            sender.sendForUser(userId, pendingIds)

            verify(pending).cancel()
            verify(fcmPushSender, never()).send(any(), any())
        }
    }

    @Nested
    inner class `발송` {

        private val token = mock<UserFcmToken>()

        private fun stubReady(pendings: List<NotificationPending>) {
            val setting = enabledSetting()
            whenever(notificationPendingRepository.findByIdInAndStatus(any(), eq(PENDING)))
                .thenReturn(pendings)
            whenever(userNotificationSettingRepository.findById(userId))
                .thenReturn(Optional.of(setting))
            whenever(userFcmTokenRepository.findById(userId)).thenReturn(Optional.of(token))
        }

        @Test
        fun `즉시 단건이면 POST_MEAL로 발송하고 SENT 처리한다`() {
            val pending = mock<NotificationPending>().also {
                whenever(it.delayed).thenReturn(false)
                whenever(it.mealRecordId).thenReturn(10L)
            }
            stubReady(listOf(pending))

            sender.sendForUser(userId, pendingIds)

            val captor = argumentCaptor<FcmPayload>()
            verify(fcmPushSender).send(eq(token), captor.capture())
            assertThat(captor.firstValue.type).isEqualTo(NotificationType.POST_MEAL)
            verify(pending).markSent()
        }

        @Test
        fun `지연 단건이면 POST_MEAL_DELAYED_SINGLE로 발송한다`() {
            val pending = mock<NotificationPending>().also {
                whenever(it.delayed).thenReturn(true)
                whenever(it.mealRecordId).thenReturn(10L)
            }
            stubReady(listOf(pending))

            sender.sendForUser(userId, pendingIds)

            val captor = argumentCaptor<FcmPayload>()
            verify(fcmPushSender).send(eq(token), captor.capture())
            assertThat(captor.firstValue.type).isEqualTo(NotificationType.POST_MEAL_DELAYED_SINGLE)
            verify(pending).markSent()
        }

        @Test
        fun `여러 건이면 POST_MEAL_DELAYED_BULK로 묶어 발송한다`() {
            val first = mock<NotificationPending>()
            val second = mock<NotificationPending>()
            stubReady(listOf(first, second))

            sender.sendForUser(userId, pendingIds)

            val captor = argumentCaptor<FcmPayload>()
            verify(fcmPushSender).send(eq(token), captor.capture())
            assertThat(captor.firstValue.type).isEqualTo(NotificationType.POST_MEAL_DELAYED_BULK)
            verify(first).markSent()
            verify(second).markSent()
        }
    }
}
