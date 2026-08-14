package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.global.config.properties.GoogleProperties
import org.springframework.stereotype.Component

@Component
class GoogleOidcVerifier(
    jwksPublicKeyProvider: JwksPublicKeyProvider,
    private val googleProperties: GoogleProperties,
) : AbstractOidcVerifier(jwksPublicKeyProvider) {

    override val provider = AuthProvider.GOOGLE
    override val jwksUrl get() = googleProperties.jwksUrl

    // 레거시 토큰은 스킴 없는 accounts.google.com으로 iss를 발급하기도 한다 — 둘 다 허용
    override val validIssuers get() = setOf(googleProperties.iss, googleProperties.iss.removePrefix("https://"))
    override val validAudiences get() = setOf(googleProperties.androidClientId, googleProperties.iosClientId)
}
