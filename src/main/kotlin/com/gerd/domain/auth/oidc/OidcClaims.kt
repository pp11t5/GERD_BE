package com.gerd.domain.auth.oidc

/**
 * OIDC ID 토큰에서 추출한 사용자 클레임
 */
data class OidcClaims(
    val sub: String,
    val email: String?,
    val nickname: String?,
    val picture: String?,
)
