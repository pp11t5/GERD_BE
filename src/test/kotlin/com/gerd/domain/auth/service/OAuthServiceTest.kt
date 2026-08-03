package com.gerd.domain.auth.service

import com.gerd.domain.auth.client.AppleApiClient
import com.gerd.domain.auth.dto.AppleTokenResponseDTO
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.oidc.OidcClaims
import com.gerd.domain.auth.oidc.OidcVerifier
import com.gerd.domain.auth.oidc.OidcVerifierRegistry
import com.gerd.domain.auth.util.AppleLoginUtil
import com.gerd.domain.auth.util.ProviderTokenUtil
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.AuthTokenFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class OAuthServiceTest {

    @Mock private lateinit var oidcVerifierRegistry: OidcVerifierRegistry
    @Mock private lateinit var appleApiClient: AppleApiClient
    @Mock private lateinit var appleLoginUtil: AppleLoginUtil
    @Mock private lateinit var providerTokenUtil: ProviderTokenUtil
    @Mock private lateinit var oAuthLoginTransactionService: OAuthLoginTransactionService

    @InjectMocks private lateinit var oAuthService: OAuthService

    @Nested
    inner class `appleLogin` {

        @Test
        fun `Authorization Code를 교환하고 nonce를 검증한 뒤 저장 트랜잭션을 호출한다`() {
            val authorizationCode = "authorization-code"
            val nonce = "apple-login-nonce"
            val idToken = "apple-id-token"
            val claims = OidcClaims(sub = "apple-123", email = "apple@test.com")
            val oidcVerifier = mock<OidcVerifier>()
            val authToken = AuthTokenFixture.userTokenResponse()
            val appleToken = AppleTokenResponseDTO(
                accessToken = "apple-access-token",
                expiresIn = 3600,
                idToken = idToken,
                refreshToken = "apple-refresh-token",
                tokenType = "bearer",
            )

            whenever(appleLoginUtil.generateClientSecret()).thenReturn("client-secret")
            whenever(appleApiClient.issueToken(authorizationCode, "client-secret")).thenReturn(appleToken)
            whenever(oidcVerifierRegistry.resolve(AuthProvider.APPLE)).thenReturn(oidcVerifier)
            whenever(oidcVerifier.verify(idToken, nonce)).thenReturn(claims)
            whenever(providerTokenUtil.encrypt("apple-refresh-token")).thenReturn("encrypted-refresh-token")
            whenever(
                oAuthLoginTransactionService.login(
                    AuthProvider.APPLE,
                    claims,
                    "encrypted-refresh-token",
                ),
            ).thenReturn(authToken)

            val result = oAuthService.appleLogin(authorizationCode, nonce)

            assertThat(result).isEqualTo(authToken)
            verify(providerTokenUtil).validateConfiguration()
            verify(oidcVerifier).verify(idToken, nonce)
            verify(oAuthLoginTransactionService).login(
                AuthProvider.APPLE,
                claims,
                "encrypted-refresh-token",
            )
        }

        @Test
        fun `Apple 응답의 refresh token이 비어 있으면 저장 트랜잭션 전에 실패한다`() {
            val appleToken = AppleTokenResponseDTO(
                accessToken = "apple-access-token",
                expiresIn = 3600,
                idToken = "apple-id-token",
                refreshToken = "",
                tokenType = "bearer",
            )
            whenever(appleLoginUtil.generateClientSecret()).thenReturn("client-secret")
            whenever(appleApiClient.issueToken("authorization-code", "client-secret")).thenReturn(appleToken)

            assertThatThrownBy { oAuthService.appleLogin("authorization-code", "nonce") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED)

            verifyNoInteractions(oidcVerifierRegistry, oAuthLoginTransactionService)
        }
    }

    @Test
    fun `소셜 토큰 검증 후 저장 트랜잭션을 호출한다`() {
        val provider = AuthProvider.KAKAO
        val idToken = "id-token"
        val claims = OidcClaims(sub = "kakao-123", email = "user@test.com")
        val verifier = mock<OidcVerifier>()
        val response = AuthTokenFixture.userTokenResponse()
        whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(verifier)
        whenever(verifier.verify(idToken)).thenReturn(claims)
        whenever(oAuthLoginTransactionService.login(provider, claims)).thenReturn(response)

        val result = oAuthService.socialLogin(provider, idToken)

        assertThat(result).isEqualTo(response)
        verify(oAuthLoginTransactionService).login(provider, claims)
    }

    @Test
    fun `복구 토큰 검증 후 복구 트랜잭션을 호출한다`() {
        val provider = AuthProvider.KAKAO
        val idToken = "id-token"
        val claims = OidcClaims(sub = "kakao-123", email = "user@test.com")
        val verifier = mock<OidcVerifier>()
        val response = AuthTokenFixture.userTokenResponse()
        whenever(oidcVerifierRegistry.resolve(provider)).thenReturn(verifier)
        whenever(verifier.verify(idToken)).thenReturn(claims)
        whenever(oAuthLoginTransactionService.recover(provider, claims)).thenReturn(response)

        val result = oAuthService.recoverAccount(provider, idToken)

        assertThat(result).isEqualTo(response)
        verify(oAuthLoginTransactionService).recover(provider, claims)
    }
}
