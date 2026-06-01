package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Component

// OidcVerifier 구현체를 provider 기준으로 자동 수집해 분기
@Component
class OidcVerifierRegistry(verifiers: List<OidcVerifier>) {

    private val verifierMap: Map<AuthProvider, OidcVerifier> = verifiers.associateBy { it.provider }

    fun resolve(provider: AuthProvider): OidcVerifier =
        verifierMap[provider] ?: throw GeneralException(AuthErrorCode.UNSUPPORTED_PROVIDER)
}
