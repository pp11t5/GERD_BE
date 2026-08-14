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

    // 앱이 requestIdToken/serverClientID로 Web Client ID를 넘기면 발급된 ID Token의 aud는 Web Client ID다 —
    // 실제로는 이 값 하나만 오지만, 클라이언트 구현이 Android/iOS Client ID를 audience로 넘기는 경우까지 대비해 함께 허용한다
    override val validAudiences get() = setOf(
        googleProperties.webClientId,
        googleProperties.androidClientId,
        googleProperties.iosClientId,
    )
}
