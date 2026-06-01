package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.global.config.properties.KakaoProperties
import org.springframework.stereotype.Component

/**
 * 카카오 OIDC 검증기
 * - JWKS URL, iss, aud는 카카오 OIDC 명세에 따라 고정
 * - JwksPublicKeyProvider를 통해 JWKS에서 공개키를 가져와 토큰 검증에 사용
 */
@Component
class KakaoOidcVerifier(
    jwksPublicKeyProvider: JwksPublicKeyProvider,
    private val kakaoProperties: KakaoProperties,
) : AbstractOidcVerifier(jwksPublicKeyProvider) {

    override val provider = AuthProvider.KAKAO
    override val jwksUrl get() = kakaoProperties.jwksUrl
    override val iss get() = kakaoProperties.iss
    override val aud get() = kakaoProperties.nativeAppKey
}
