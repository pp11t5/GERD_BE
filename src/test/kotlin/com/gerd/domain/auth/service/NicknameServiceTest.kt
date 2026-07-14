package com.gerd.domain.auth.service

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.springframework.dao.DataIntegrityViolationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class NicknameServiceTest {

    @Mock private lateinit var userRepository: UserRepository

    @InjectMocks private lateinit var nicknameService: NicknameService

    @Nested
    inner class `generateUniqueNickname` {

        @Test
        fun `겹치는 닉네임이 없으면 원본 조합을 그대로 반환한다`() {
            whenever(userRepository.findNicknamesByBaseIncludingDeleted(any())).thenReturn(emptyList())

            val result = nicknameService.generateUniqueNickname()

            assertThat(result).matches("^\\S+ \\S+$")
        }

        @Test
        fun `원본만 있으면 1을 붙인다`() {
            whenever(userRepository.findNicknamesByBaseIncludingDeleted(any()))
                .thenAnswer { invocation ->
                    val base = invocation.getArgument<String>(0)
                    listOf(base)
                }

            val result = nicknameService.generateUniqueNickname()

            assertThat(result).matches("^\\S+ \\S+1$")
        }

        @Test
        fun `1과 2가 있으면 3을 붙인다`() {
            whenever(userRepository.findNicknamesByBaseIncludingDeleted(any()))
                .thenAnswer { invocation ->
                    val base = invocation.getArgument<String>(0)
                    listOf(base, "${base}1", "${base}2")
                }

            val result = nicknameService.generateUniqueNickname()

            assertThat(result).matches("^\\S+ \\S+3$")
        }
    }

    @Nested
    inner class `changeNickname` {

        private val userId = 1L

        @Test
        fun `본인을 제외하고 중복이 없으면 닉네임을 변경한다`() {
            val user = UserFixture.user()
            whenever(userRepository.existsByNicknameIncludingDeleted("다정한 기린", userId)).thenReturn(false)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

            nicknameService.changeNickname(userId, "다정한 기린")

            assertThat(user.nickname).isEqualTo("다정한 기린")
        }

        @Test
        fun `본인을 제외한 다른 유저가 이미 쓰는 닉네임이면 NICKNAME_ALREADY_IN_USE를 던진다`() {
            whenever(userRepository.existsByNicknameIncludingDeleted("다정한 기린", userId)).thenReturn(true)

            assertThatThrownBy { nicknameService.changeNickname(userId, "다정한 기린") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.NICKNAME_ALREADY_IN_USE)

            verify(userRepository, never()).findById(eq(userId))
        }

        @Test
        fun `exists 체크를 통과했지만 저장 시점에 DB unique 제약을 위반하면 NICKNAME_ALREADY_IN_USE를 던진다`() {
            val user = UserFixture.user()
            whenever(userRepository.existsByNicknameIncludingDeleted("다정한 기린", userId)).thenReturn(false)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(userRepository.saveAndFlush(user))
                .thenThrow(DataIntegrityViolationException("duplicate key"))

            assertThatThrownBy { nicknameService.changeNickname(userId, "다정한 기린") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.NICKNAME_ALREADY_IN_USE)
        }
    }
}
