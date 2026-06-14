package com.gerd.domain.notification.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.fcm.service.FcmPushSender
import com.gerd.domain.notification.entity.enums.NotificationType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl

@ExtendWith(MockitoExtension::class)
class WeeklyReportNotificationUseCaseTest {

    @Mock
    private lateinit var userFcmTokenRepository: UserFcmTokenRepository

    @Mock
    private lateinit var fcmPushSender: FcmPushSender

    @InjectMocks
    private lateinit var weeklyReportNotificationUseCase: WeeklyReportNotificationUseCase

    private fun token(userId: Long, token: String): UserFcmToken =
        UserFcmToken(
            user = User(id = userId, email = "user$userId@test.com", role = UserRole.USER),
            userId = userId,
            platform = DevicePlatform.IOS,
            token = token,
        )

    @Nested
    inner class `send` {

        @Test
        fun `커서 기반으로 주간 리포트 대상 토큰을 나누어 발송한다`() {
            val pageable = PageRequest.of(0, NotificationBatchPolicy.FCM_MULTICAST_BATCH_SIZE)
            whenever(userFcmTokenRepository.findByWeeklyReportEnabled(0L, pageable))
                .thenReturn(SliceImpl(listOf(token(1L, "token-1"), token(2L, "token-2")), pageable, true))
            whenever(userFcmTokenRepository.findByWeeklyReportEnabled(2L, pageable))
                .thenReturn(SliceImpl(listOf(token(3L, "token-3")), pageable, false))

            weeklyReportNotificationUseCase.send()

            verify(fcmPushSender).sendMulticast(
                eq(listOf("token-1", "token-2")),
                argThat { type == NotificationType.WEEKLY_REPORT },
            )
            verify(fcmPushSender).sendMulticast(
                eq(listOf("token-3")),
                argThat { type == NotificationType.WEEKLY_REPORT },
            )
        }

        @Test
        fun `대상 토큰이 없으면 발송하지 않는다`() {
            val pageable = PageRequest.of(0, NotificationBatchPolicy.FCM_MULTICAST_BATCH_SIZE)
            whenever(userFcmTokenRepository.findByWeeklyReportEnabled(0L, pageable))
                .thenReturn(SliceImpl(emptyList(), pageable, false))

            weeklyReportNotificationUseCase.send()

            verify(fcmPushSender, never()).sendMulticast(any(), any())
        }
    }
}
