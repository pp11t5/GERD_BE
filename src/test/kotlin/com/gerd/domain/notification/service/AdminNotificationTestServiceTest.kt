package com.gerd.domain.notification.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.fcm.service.FcmPushSender
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.global.apiPayload.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AdminNotificationTestServiceTest {

    @Mock private lateinit var userFcmTokenRepository: UserFcmTokenRepository
    @Mock private lateinit var fcmPushSender: FcmPushSender

    @InjectMocks private lateinit var service: AdminNotificationTestService

    private val userId = 1L

    @Test
    fun `FCM 토큰이 없으면 FCM_TOKEN_NOT_FOUND`() {
        whenever(userFcmTokenRepository.existsById(userId)).thenReturn(false)

        assertThatThrownBy { service.send(userId, NotificationType.POST_MEAL, null) }
            .isInstanceOf(GeneralException::class.java)
            .extracting("errorCode").isEqualTo(FcmErrorCode.FCM_TOKEN_NOT_FOUND)
        verify(fcmPushSender, never()).sendToUser(any(), any())
    }

    @Test
    fun `토큰이 있으면 실제 문구 그대로 sendToUser로 발송한다`() {
        whenever(userFcmTokenRepository.existsById(userId)).thenReturn(true)

        service.send(userId, NotificationType.POST_MEAL, "100")

        val captor = argumentCaptor<FcmPayload>()
        verify(fcmPushSender).sendToUser(eq(userId), captor.capture())
        assertThat(captor.firstValue.type).isEqualTo(NotificationType.POST_MEAL)
        assertThat(captor.firstValue.targetId).isEqualTo("100")
    }
}
