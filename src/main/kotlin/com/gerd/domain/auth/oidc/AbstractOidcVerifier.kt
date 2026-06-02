package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import java.security.interfaces.RSAPublicKey
import java.util.Base64

/**
 * OIDC 검증 로직을 공통화한 추상 클래스
 */
abstract class AbstractOidcVerifier(
    private val jwksPublicKeyProvider: JwksPublicKeyProvider,
) : OidcVerifier {

    protected abstract val jwksUrl: String
    protected abstract val iss: String
    protected abstract val aud: String

    override fun verify(idToken: String): OidcClaims {
        val kid = extractKid(idToken)
        val publicKey = findPublicKey(kid)
        return extractClaims(idToken, publicKey)
    }

    // 검증 없이 헤더만 Base64 디코딩해 kid 추출
    private fun extractKid(idToken: String): String {
        return try {
            val header = String(Base64.getUrlDecoder().decode(idToken.split(".")[0]))
            Regex("\"kid\"\\s*:\\s*\"([^\"]+)\"").find(header)?.groupValues?.get(1)
                ?: throw GeneralException(AuthErrorCode.INVALID_TOKEN)
        } catch (e: GeneralException) {
            throw e
        } catch (e: Exception) {
            throw GeneralException(AuthErrorCode.INVALID_TOKEN)
        }
    }

    // kid 일치 공개키 탐색 — stale 캐시면 무효화 후 1회 재시도
    private fun findPublicKey(kid: String): RSAPublicKey {
        fun findIn(): RSAPublicKey? =
            jwksPublicKeyProvider.getKeys(jwksUrl)
                ?.keys
                ?.find { it.kid == kid }
                ?.let { RsaPublicKeyFactory.create(it.n, it.e) }

        return findIn()
            ?: run {
                jwksPublicKeyProvider.invalidate(jwksUrl)
                findIn() ?: throw GeneralException(AuthErrorCode.INVALID_TOKEN)
            }
    }

    // iss, aud, exp, 서명 한 번에 검증 후 클레임 추출
    private fun extractClaims(idToken: String, publicKey: RSAPublicKey): OidcClaims {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(iss)
                .requireAudience(aud)
                .build()
                .parseClaimsJws(idToken)
                .body

            OidcClaims(
                sub = claims.subject,
                email = claims["email"] as? String,
                nickname = extractNickname(claims),
                picture = claims["picture"] as? String,
            )
        } catch (e: ExpiredJwtException) {
            throw GeneralException(AuthErrorCode.EXPIRED_TOKEN)
        } catch (e: JwtException) {
            throw GeneralException(AuthErrorCode.INVALID_TOKEN)
        }
    }

    // 프로바이더마다 닉네임 클레임 키가 다르므로 재정의 허용
    protected open fun extractNickname(claims: Claims): String? = claims["nickname"] as? String
}
