package com.gerd.domain.auth.service

import com.gerd.domain.auth.client.AppleApiClient
import com.gerd.domain.auth.client.KakaoApiClient
import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.RefreshTokenRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.auth.security.AccessTokenBlacklist
import com.gerd.domain.auth.util.ProviderTokenUtil
import com.gerd.global.fixture.UserFixture
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.scheduling.TaskScheduler
import java.time.Duration
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class WithdrawServiceTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var authAccountRepository: AuthAccountRepository
    @Mock private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Mock private lateinit var accessTokenBlacklist: AccessTokenBlacklist
    @Mock private lateinit var kakaoApiClient: KakaoApiClient
    @Mock private lateinit var appleApiClient: AppleApiClient
    @Mock private lateinit var providerTokenUtil: ProviderTokenUtil
    @Mock private lateinit var taskScheduler: TaskScheduler

    private lateinit var withdrawService: WithdrawService

    @BeforeEach
    fun setUp() {
        withdrawService = WithdrawService(
            userRepository = userRepository,
            authAccountRepository = authAccountRepository,
            refreshTokenRepository = refreshTokenRepository,
            accessTokenBlacklist = accessTokenBlacklist,
            kakaoApiClient = kakaoApiClient,
            appleApiClient = appleApiClient,
            providerTokenUtil = providerTokenUtil,
            taskScheduler = taskScheduler,
            gracePeriod = Duration.ofDays(14),
            scheduleInMemory = false,
        )
    }

    @Nested
    inner class `withdrawHardDelete` {

        @Test
        fun `Apple 계정이면 refresh token을 복호화해 revoke한 후 물리 삭제한다`() {
            val userId = 1L
            val authAccount = AuthAccount(
                userId = userId,
                user = UserFixture.user(),
                provider = AuthProvider.APPLE,
                providerAccountId = "apple-123",
            ).apply {
                updateProviderRefreshToken("encrypted-refresh-token")
            }
            whenever(authAccountRepository.findById(userId)).thenReturn(Optional.of(authAccount))
            whenever(providerTokenUtil.decrypt("encrypted-refresh-token")).thenReturn("apple-refresh-token")

            withdrawService.withdrawHardDelete(userId)

            verify(appleApiClient).revoke("apple-refresh-token")
            verify(kakaoApiClient, never()).unlink(org.mockito.kotlin.any())
            verify(userRepository).hardDelete(userId)
        }

        @Test
        fun `Kakao 계정이면 unlink한 후 기존 물리 삭제 흐름을 유지한다`() {
            val userId = 2L
            val authAccount = AuthAccount(
                userId = userId,
                user = UserFixture.user(),
                provider = AuthProvider.KAKAO,
                providerAccountId = "kakao-123",
            )
            whenever(authAccountRepository.findById(userId)).thenReturn(Optional.of(authAccount))

            withdrawService.withdrawHardDelete(userId)

            verify(kakaoApiClient).unlink("kakao-123")
            verify(appleApiClient, never()).revoke(org.mockito.kotlin.any())
            verify(userRepository).hardDelete(userId)
        }

        @Test
        fun `Google 계정이면 별도 revoke 없이 물리 삭제한다`() {
            val userId = 4L
            val authAccount = AuthAccount(
                userId = userId,
                user = UserFixture.user(),
                provider = AuthProvider.GOOGLE,
                providerAccountId = "google-123",
            )
            whenever(authAccountRepository.findById(userId)).thenReturn(Optional.of(authAccount))

            withdrawService.withdrawHardDelete(userId)

            verify(kakaoApiClient, never()).unlink(org.mockito.kotlin.any())
            verify(appleApiClient, never()).revoke(org.mockito.kotlin.any())
            verify(userRepository).hardDelete(userId)
        }

        @Test
        fun `Apple revoke에 실패하면 물리 삭제하지 않아 다음 배치에서 재시도할 수 있다`() {
            val userId = 3L
            val authAccount = AuthAccount(
                userId = userId,
                user = UserFixture.user(),
                provider = AuthProvider.APPLE,
                providerAccountId = "apple-456",
            ).apply {
                updateProviderRefreshToken("encrypted-refresh-token")
            }
            whenever(authAccountRepository.findById(userId)).thenReturn(Optional.of(authAccount))
            whenever(providerTokenUtil.decrypt("encrypted-refresh-token")).thenReturn("apple-refresh-token")
            doThrow(IllegalStateException("revoke failed"))
                .whenever(appleApiClient)
                .revoke("apple-refresh-token")

            assertThatThrownBy { withdrawService.withdrawHardDelete(userId) }
                .isInstanceOf(IllegalStateException::class.java)

            verify(userRepository, never()).hardDelete(userId)
        }
    }
}
