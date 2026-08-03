package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.entity.enums.UserStatus
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.oidc.OidcClaims
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.AuthTokenFixture
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OAuthLoginTransactionServiceTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var authAccountRepository: AuthAccountRepository
    @Mock private lateinit var authService: AuthService
    @Mock private lateinit var userAccountRegistrar: UserAccountRegistrar
    @Mock private lateinit var nicknameService: NicknameService

    @InjectMocks private lateinit var service: OAuthLoginTransactionService

    private val provider = AuthProvider.KAKAO
    private val claims = OidcClaims(sub = "provider-123", email = "user@test.com")

    @Nested
    inner class `login` {

        @Test
        fun `기존 사용자는 마지막 로그인 시각을 갱신하고 토큰을 발급한다`() {
            val user = UserFixture.user()
            val account = AuthAccount(userId = 1L, user = user, provider = provider, providerAccountId = claims.sub)
            val response = AuthTokenFixture.userTokenResponse()
            whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                .thenReturn(Optional.of(account))
            whenever(userRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(user))
            whenever(authService.issueTokens(user)).thenReturn(response)

            val result = service.login(provider, claims)

            assertThat(result).isEqualTo(response)
            assertThat(user.lastLoginAt).isNotNull()
        }

        @Test
        fun `신규 사용자는 가입 후 토큰을 발급한다`() {
            val user = UserFixture.user()
            val response = AuthTokenFixture.userTokenResponse()
            whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                .thenReturn(Optional.empty())
            whenever(userAccountRegistrar.findOrRegister(eq(claims.email!!), eq(provider), eq(claims.sub), any()))
                .thenReturn(1L)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            whenever(authService.issueTokens(user)).thenReturn(response)

            val result = service.login(provider, claims)

            assertThat(result).isEqualTo(response)
            verify(userAccountRegistrar).findOrRegister(eq(claims.email!!), eq(provider), eq(claims.sub), any())
        }

        @Test
        fun `Apple refresh token 암호문을 연동 계정에 저장한다`() {
            val user = UserFixture.user()
            val account = AuthAccount(userId = 1L, user = user, provider = AuthProvider.APPLE, providerAccountId = claims.sub)
            val response = AuthTokenFixture.userTokenResponse()
            whenever(authAccountRepository.findByProviderAndProviderAccountId(AuthProvider.APPLE, claims.sub))
                .thenReturn(Optional.of(account))
            whenever(userRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(user))
            whenever(authService.issueTokens(user)).thenReturn(response)

            service.login(AuthProvider.APPLE, claims, "encrypted-refresh-token")

            assertThat(account.providerRefreshToken).isEqualTo("encrypted-refresh-token")
        }

        @Test
        fun `탈퇴 유저는 ACCOUNT_RECOVERABLE을 던진다`() {
            val user = UserFixture.deletedUser()
            val account = AuthAccount(userId = 4L, user = user, provider = provider, providerAccountId = claims.sub)
            whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                .thenReturn(Optional.of(account))
            whenever(userRepository.findByIdIncludingDeleted(4L)).thenReturn(Optional.of(user))

            assertThatThrownBy { service.login(provider, claims) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_RECOVERABLE)
        }
    }

    @Nested
    inner class `recover` {

        @Test
        fun `탈퇴 유예 사용자를 복구하고 토큰을 발급한다`() {
            val user = UserFixture.deletedUser()
            val account = AuthAccount(userId = 4L, user = user, provider = provider, providerAccountId = claims.sub)
            val response = AuthTokenFixture.userTokenResponse(userId = "4")
            whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                .thenReturn(Optional.of(account))
            whenever(userRepository.findByIdIncludingDeleted(4L)).thenReturn(Optional.of(user))
            whenever(authService.issueTokens(user)).thenReturn(response)

            val result = service.recover(provider, claims)

            assertThat(result).isEqualTo(response)
            assertThat(user.status).isEqualTo(UserStatus.ACTIVE)
            assertThat(user.deletedAt).isNull()
        }

        @Test
        fun `활성 사용자는 복구할 수 없다`() {
            val user = UserFixture.user()
            val account = AuthAccount(userId = 1L, user = user, provider = provider, providerAccountId = claims.sub)
            whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                .thenReturn(Optional.of(account))
            whenever(userRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(user))

            assertThatThrownBy { service.recover(provider, claims) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
        }
    }
}
