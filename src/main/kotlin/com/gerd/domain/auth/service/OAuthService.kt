package com.gerd.domain.auth.service

import com.gerd.domain.auth.client.AppleApiClient
import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.oidc.OidcVerifierRegistry
import com.gerd.domain.auth.util.AppleLoginUtil
import com.gerd.domain.auth.util.ProviderTokenUtil
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service

/**
 * OAuth 관련 비즈니스 로직을 처리하는 서비스
 * - 로그인 처리
 * - 로그인 시 상태분기해서 예외처리
 * - 사용자 생성 처리
 */
@Service
class OAuthService(
    private val oidcVerifierRegistry: OidcVerifierRegistry,
    private val appleApiClient: AppleApiClient,
    private val appleLoginUtil: AppleLoginUtil,
    private val providerTokenUtil: ProviderTokenUtil,
    private val oAuthLoginTransactionService: OAuthLoginTransactionService,
) {

    fun appleLogin(
        authorizationCode: String,
        nonce: String,
    ): AuthTokenResponseDTO {
        providerTokenUtil.validateConfiguration()

        val appleToken = appleApiClient.issueToken(
            code = authorizationCode,
            clientSecret = appleLoginUtil.generateClientSecret(),
        )
        val refreshToken = appleToken.refreshToken
            .takeIf { it.isNotBlank() }
            ?: throw GeneralException(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED)

        val claims = oidcVerifierRegistry
            .resolve(AuthProvider.APPLE)
            .verify(appleToken.idToken, nonce)
        val encryptedRefreshToken = providerTokenUtil.encrypt(refreshToken)

        return oAuthLoginTransactionService.login(
            provider = AuthProvider.APPLE,
            claims = claims,
            encryptedProviderRefreshToken = encryptedRefreshToken,
        )
    }

    // provider별 검증기로 분기 → 기존 계정이면 로그인, 신규면 가입 후 로그인
    fun socialLogin(
        provider: AuthProvider,
        idToken: String,
    ): AuthTokenResponseDTO {
        val claims = oidcVerifierRegistry.resolve(provider).verify(idToken)

        return oAuthLoginTransactionService.login(provider, claims)
    }

    // 탈퇴 유예기간 중인 계정 복구 — 같은 idToken으로 바로 재사용 가능
    fun recoverAccount(
        provider: AuthProvider,
        idToken: String,
    ): AuthTokenResponseDTO {
        val claims = oidcVerifierRegistry.resolve(provider).verify(idToken)

        return oAuthLoginTransactionService.recover(provider, claims)
    }
}
