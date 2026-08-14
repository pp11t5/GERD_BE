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

    override val validIssuers get() = setOf(googleProperties.iss, googleProperties.iss.removePrefix("https://"))
    override val validAudiences get() = setOf(
        googleProperties.webClientId,
        googleProperties.androidClientId,
        googleProperties.iosClientId,
    )
}
