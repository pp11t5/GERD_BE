package com.gerd.domain.fcm.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.fcm.dto.FcmTokenRegisterRequestDTO
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.fcm.repository.UserFcmTokenRepository
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class FcmTokenServiceTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var userFcmTokenRepository: UserFcmTokenRepository

    @InjectMocks private lateinit var fcmTokenService: FcmTokenService

    private val user = UserFixture.user()
    private val userId = user.id!!
    private val request = FcmTokenRegisterRequestDTO(platform = DevicePlatform.IOS, token = "fcm-token-123")

    @Nested
    inner class `register` {

        @Nested
        inner class 성공 {

            @Test
            fun `기존 토큰이 있으면 updateToken으로 갱신한다`() {
                // given
                val existingToken = mock<UserFcmToken>()
                whenever(userFcmTokenRepository.findById(userId)).thenReturn(Optional.of(existingToken))

                // when
                fcmTokenService.register(userId, request)

                // then
                verify(existingToken).updateToken(request.token, request.platform)
                verify(userFcmTokenRepository, never()).save(any())
            }

            @Test
            fun `토큰이 없으면 신규 토큰을 저장한다`() {
                // given
                whenever(userFcmTokenRepository.findById(userId)).thenReturn(Optional.empty())
                whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
                whenever(userFcmTokenRepository.save(any())).thenReturn(mock())

                // when
                fcmTokenService.register(userId, request)

                // then
                verify(userFcmTokenRepository).save(any())
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `유저가 존재하지 않으면 USER_NOT_FOUND를 반환한다`() {
                // given
                whenever(userFcmTokenRepository.findById(userId)).thenReturn(Optional.empty())
                whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

                // when & then
                assertThatThrownBy { fcmTokenService.register(userId, request) }
                    .isInstanceOf(GeneralException::class.java)
                    .satisfies({ ex ->
                        assertThat((ex as GeneralException).errorCode).isEqualTo(AuthErrorCode.USER_NOT_FOUND)
                    })
            }
        }
    }

    @Nested
    inner class `delete` {

        @Nested
        inner class 성공 {

            @Test
            fun `토큰을 삭제한다`() {
                // given
                val existingToken = mock<UserFcmToken>()
                whenever(userFcmTokenRepository.findById(userId)).thenReturn(Optional.of(existingToken))

                // when
                fcmTokenService.delete(userId)

                // then
                verify(userFcmTokenRepository).delete(existingToken)
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `토큰이 없으면 FCM_TOKEN_NOT_FOUND를 반환한다`() {
                // given
                whenever(userFcmTokenRepository.findById(userId)).thenReturn(Optional.empty())

                // when & then
                assertThatThrownBy { fcmTokenService.delete(userId) }
                    .isInstanceOf(GeneralException::class.java)
                    .satisfies({ ex ->
                        assertThat((ex as GeneralException).errorCode).isEqualTo(FcmErrorCode.FCM_TOKEN_NOT_FOUND)
                    })
            }
        }
    }

    @Nested
    inner class `deleteByToken` {

        @Nested
        inner class 성공 {

            @Test
            fun `만료 토큰을 삭제한다`() {
                // given
                val existingToken = mock<UserFcmToken>()
                whenever(userFcmTokenRepository.findByToken("fcm-token-123")).thenReturn(existingToken)

                // when
                fcmTokenService.deleteByToken("fcm-token-123")

                // then
                verify(userFcmTokenRepository).delete(existingToken)
            }

            @Test
            fun `토큰이 없으면 아무 작업도 하지 않는다`() {
                // given
                whenever(userFcmTokenRepository.findByToken("expired-token")).thenReturn(null)

                // when
                fcmTokenService.deleteByToken("expired-token")

                // then
                verify(userFcmTokenRepository, never()).delete(any())
            }
        }
    }
}
