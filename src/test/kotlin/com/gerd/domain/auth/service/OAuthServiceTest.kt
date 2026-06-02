package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.entity.enums.UserStatus
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.oidc.OidcClaims
import com.gerd.domain.auth.oidc.OidcVerifier
import com.gerd.domain.auth.oidc.OidcVerifierRegistry
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OAuthServiceTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var authAccountRepository: AuthAccountRepository
    @Mock private lateinit var authService: AuthService
    @Mock private lateinit var oidcVerifierRegistry: OidcVerifierRegistry
    @Mock private lateinit var userAccountRegistrar: UserAccountRegistrar

    @InjectMocks private lateinit var oAuthService: OAuthService

    @Nested
    inner class `socialLogin` {

        private val idToken = "id.token"
        private val provider = AuthProvider.KAKAO
        private val claims = OidcClaims(sub = "kakao-123", email = "user@test.com", nickname = "dev-user", picture = null)

        @Nested
        inner class `성공` {

            @Test
            fun `기존 ACTIVE 유저는 토큰을 재발급하고 lastLoginAt을 갱신한다`() {
                val user = UserFixture.user()
                val authAccount = AuthAccount(userId = 1L, user = user, provider = provider, providerAccountId = claims.sub)
                val oidcVerifier = mock<OidcVerifier>()
                val tokenResponse = AuthTokenFixture.userTokenResponse()

                whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(oidcVerifier)
                whenever(oidcVerifier.verify(idToken)).thenReturn(claims)
                whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                    .thenReturn(Optional.of(authAccount))
                whenever(userRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(user))
                whenever(authService.issueTokens(user)).thenReturn(tokenResponse)

                val result = oAuthService.socialLogin(provider, idToken)

                assertThat(result).isEqualTo(tokenResponse)
                assertThat(user.lastLoginAt).isNotNull()
                verify(userRepository).findByIdIncludingDeleted(1L)
            }

            @Test
            fun `신규 유저는 가입 후 토큰을 발급한다`() {
                val newUser = UserFixture.user()
                val oidcVerifier = mock<OidcVerifier>()
                val tokenResponse = AuthTokenFixture.userTokenResponse()

                whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(oidcVerifier)
                whenever(oidcVerifier.verify(idToken)).thenReturn(claims)
                whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                    .thenReturn(Optional.empty())
                whenever(userAccountRegistrar.findOrRegister(eq(claims.email!!), eq(provider), eq(claims.sub), any()))
                    .thenReturn(1L)
                whenever(userRepository.findById(1L)).thenReturn(Optional.of(newUser))
                whenever(authService.issueTokens(newUser)).thenReturn(tokenResponse)

                val result = oAuthService.socialLogin(provider, idToken)

                assertThat(result).isEqualTo(tokenResponse)
                verify(userAccountRegistrar).findOrRegister(eq(claims.email!!), eq(provider), eq(claims.sub), any())
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `탈퇴(DELETED) 유저가 로그인 시도 시 ACCOUNT_RECOVERABLE을 던진다`() {
                // 회귀: authAccount.user LAZY 프록시 접근 시 EntityNotFoundException(500) 대신 403 반환 검증
                val deletedUser = UserFixture.deletedUser()
                val authAccount = AuthAccount(userId = 4L, user = deletedUser, provider = provider, providerAccountId = claims.sub)
                val oidcVerifier = mock<OidcVerifier>()

                whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(oidcVerifier)
                whenever(oidcVerifier.verify(idToken)).thenReturn(claims)
                whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                    .thenReturn(Optional.of(authAccount))
                whenever(userRepository.findByIdIncludingDeleted(4L)).thenReturn(Optional.of(deletedUser))

                assertThatThrownBy { oAuthService.socialLogin(provider, idToken) }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.ACCOUNT_RECOVERABLE)

                verify(userRepository).findByIdIncludingDeleted(4L)
            }

            @Test
            fun `신규 유저에 email claim이 없으면 EMAIL_REQUIRED를 던진다`() {
                val claimsWithoutEmail = OidcClaims(sub = "kakao-123", email = null, nickname = "dev-user", picture = null)
                val oidcVerifier = mock<OidcVerifier>()

                whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(oidcVerifier)
                whenever(oidcVerifier.verify(idToken)).thenReturn(claimsWithoutEmail)
                whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claimsWithoutEmail.sub))
                    .thenReturn(Optional.empty())

                assertThatThrownBy { oAuthService.socialLogin(provider, idToken) }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.EMAIL_REQUIRED)
            }
        }
    }

    @Nested
    inner class `recoverAccount` {

        private val idToken = "id.token"
        private val provider = AuthProvider.KAKAO
        private val claims = OidcClaims(sub = "kakao-456", email = "deleted@test.com", nickname = "deleted-user", picture = null)

        @Nested
        inner class `성공` {

            @Test
            fun `탈퇴 유예기간 중인 유저를 ACTIVE로 복구하고 토큰을 발급한다`() {
                val deletedUser = UserFixture.deletedUser()
                val authAccount = AuthAccount(userId = 4L, user = deletedUser, provider = provider, providerAccountId = claims.sub)
                val oidcVerifier = mock<OidcVerifier>()
                val tokenResponse = AuthTokenFixture.userTokenResponse(userId = "4", email = "deleted@test.com")

                whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(oidcVerifier)
                whenever(oidcVerifier.verify(idToken)).thenReturn(claims)
                whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                    .thenReturn(Optional.of(authAccount))
                whenever(userRepository.findByIdIncludingDeleted(4L)).thenReturn(Optional.of(deletedUser))
                whenever(authService.issueTokens(deletedUser)).thenReturn(tokenResponse)

                val result = oAuthService.recoverAccount(provider, idToken)

                assertThat(result).isEqualTo(tokenResponse)
                assertThat(deletedUser.status).isEqualTo(UserStatus.ACTIVE)
                assertThat(deletedUser.deletedAt).isNull()
            }
        }

        @Nested
        inner class `실패` {

            @Test
            fun `authAccount가 없으면 USER_NOT_FOUND를 던진다`() {
                val oidcVerifier = mock<OidcVerifier>()

                whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(oidcVerifier)
                whenever(oidcVerifier.verify(idToken)).thenReturn(claims)
                whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                    .thenReturn(Optional.empty())

                assertThatThrownBy { oAuthService.recoverAccount(provider, idToken) }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
            }

            @Test
            fun `ACTIVE 유저로 복구 시도 시 USER_NOT_FOUND를 던진다`() {
                val activeUser = UserFixture.user()
                val authAccount = AuthAccount(userId = 1L, user = activeUser, provider = provider, providerAccountId = claims.sub)
                val oidcVerifier = mock<OidcVerifier>()

                whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(oidcVerifier)
                whenever(oidcVerifier.verify(idToken)).thenReturn(claims)
                whenever(authAccountRepository.findByProviderAndProviderAccountId(provider, claims.sub))
                    .thenReturn(Optional.of(authAccount))
                whenever(userRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(activeUser))

                assertThatThrownBy { oAuthService.recoverAccount(provider, idToken) }
                    .isInstanceOf(GeneralException::class.java)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND)
            }
        }
    }
}
