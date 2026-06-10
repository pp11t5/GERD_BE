package com.gerd.domain.fcm.service

import com.gerd.domain.fcm.dto.FcmPayload
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
import com.gerd.domain.notification.entity.enums.NotificationType
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class FcmClientTest {

    @Mock private lateinit var fcmMessageFactory: FcmMessageFactory
    @Mock private lateinit var fcmTokenService: FcmTokenService
    @Mock private lateinit var fcmTokenRepository: UserFcmTokenRepository
    @Mock private lateinit var firebaseMessaging: FirebaseMessaging

    @InjectMocks private lateinit var fcmClient: FcmClient

    private val userId = 1L
    private val payload = FcmPayload(
        title = "테스트",
        body = "테스트 내용",
        type = NotificationType.POST_MEAL,
    )

    @Nested
    inner class `sendToUser` {

        @Nested
        inner class 성공 {

            @Test
            fun `토큰이 없으면 조용히 스킵한다`() {
                whenever(fcmTokenRepository.findById(userId)).thenReturn(Optional.empty())

                fcmClient.sendToUser(userId, payload)

                verify(firebaseMessaging, never()).send(any<Message>())
            }

            @Test
            fun `토큰이 있으면 메시지를 빌드하고 발송한다`() {
                val token = mock<UserFcmToken>().also {
                    whenever(it.token).thenReturn("fcm-token")
                    whenever(it.platform).thenReturn(DevicePlatform.IOS)
                }
                val message = mock<Message>()
                whenever(fcmTokenRepository.findById(userId)).thenReturn(Optional.of(token))
                whenever(fcmMessageFactory.build(any(), any(), any())).thenReturn(message)
                whenever(firebaseMessaging.send(message)).thenReturn("message-id")

                fcmClient.sendToUser(userId, payload)

                verify(firebaseMessaging).send(message)
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `UNREGISTERED 에러면 만료 토큰을 삭제한다`() {
                val token = mock<UserFcmToken>().also {
                    whenever(it.token).thenReturn("fcm-token")
                    whenever(it.platform).thenReturn(DevicePlatform.IOS)
                }
                val message = mock<Message>()
                val exception = mock<FirebaseMessagingException>().also {
                    whenever(it.messagingErrorCode).thenReturn(MessagingErrorCode.UNREGISTERED)
                }
                whenever(fcmTokenRepository.findById(userId)).thenReturn(Optional.of(token))
                whenever(fcmMessageFactory.build(any(), any(), any())).thenReturn(message)
                whenever(firebaseMessaging.send(message)).thenThrow(exception)
                doNothing().whenever(fcmTokenService).deleteByToken(any())

                fcmClient.sendToUser(userId, payload)

                verify(fcmTokenService).deleteByToken("fcm-token")
            }

            @Test
            fun `UNREGISTERED 외 에러면 토큰을 삭제하지 않는다`() {
                val token = mock<UserFcmToken>().also {
                    whenever(it.token).thenReturn("fcm-token")
                    whenever(it.platform).thenReturn(DevicePlatform.IOS)
                }
                val message = mock<Message>()
                val exception = mock<FirebaseMessagingException>().also {
                    whenever(it.messagingErrorCode).thenReturn(MessagingErrorCode.INTERNAL)
                }
                whenever(fcmTokenRepository.findById(userId)).thenReturn(Optional.of(token))
                whenever(fcmMessageFactory.build(any(), any(), any())).thenReturn(message)
                whenever(firebaseMessaging.send(message)).thenThrow(exception)

                fcmClient.sendToUser(userId, payload)

                verify(fcmTokenService, never()).deleteByToken(any())
            }
        }
    }

    @Nested
    inner class `sendToTopic` {

        @Test
        fun `토픽으로 메시지를 발송한다`() {
            val message = mock<Message>()
            whenever(fcmMessageFactory.buildTopic(any(), any())).thenReturn(message)
            whenever(firebaseMessaging.send(message)).thenReturn("message-id")

            fcmClient.sendToTopic("weekly-report", payload)

            verify(firebaseMessaging).send(message)
        }

        @Test
        fun `발송 실패해도 예외를 전파하지 않는다`() {
            val message = mock<Message>()
            val exception = mock<FirebaseMessagingException>().also {
                whenever(it.messagingErrorCode).thenReturn(MessagingErrorCode.INTERNAL)
            }
            whenever(fcmMessageFactory.buildTopic(any(), any())).thenReturn(message)
            whenever(firebaseMessaging.send(message)).thenThrow(exception)

            fcmClient.sendToTopic("weekly-report", payload)

            verify(firebaseMessaging).send(message)
        }
    }

    @Nested
    inner class `subscribeToTopic` {

        @Test
        fun `토픽을 구독한다`() {
            whenever(firebaseMessaging.subscribeToTopic(any(), any())).thenReturn(mock())

            fcmClient.subscribeToTopic("fcm-token", "weekly-report")

            verify(firebaseMessaging).subscribeToTopic(listOf("fcm-token"), "weekly-report")
        }
    }

    @Nested
    inner class `unsubscribeFromTopic` {

        @Test
        fun `토픽 구독을 해제한다`() {
            whenever(firebaseMessaging.unsubscribeFromTopic(any(), any())).thenReturn(mock())

            fcmClient.unsubscribeFromTopic("fcm-token", "weekly-report")

            verify(firebaseMessaging).unsubscribeFromTopic(listOf("fcm-token"), "weekly-report")
        }
    }
}
