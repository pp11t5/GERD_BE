package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.global.config.properties.AppleProperties
import org.springframework.stereotype.Component

@Component
class AppleOidcVerifier(
    jwksPublicKeyProvider: JwksPublicKeyProvider,
    private val appleProperties: AppleProperties,
) : AbstractOidcVerifier(jwksPublicKeyProvider) {

    override val provider = AuthProvider.APPLE
    override val jwksUrl get() = appleProperties.jwksUrl
    override val iss get() = appleProperties.iss
    override val validAudiences get() = setOf(appleProperties.clientId)
}
