package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.entity.enums.AuthProvider

/**
 * 소셜 로그인 수행하는 공용 Verifier
 * 소셜 로그인 플랫폼이 추가되면 해당 인터페이스를 구현하는 방식
 */
interface OidcVerifier {
    val provider: AuthProvider
    fun verify(idToken: String): OidcClaims
}
