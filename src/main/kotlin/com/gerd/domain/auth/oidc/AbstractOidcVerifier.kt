package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import java.security.interfaces.RSAPublicKey
import java.util.Base64

private val log = KotlinLogging.logger {}

/**
 * OIDC 검증 로직을 공통화한 추상 클래스
 */
abstract class AbstractOidcVerifier(
    private val jwksPublicKeyProvider: JwksPublicKeyProvider,
) : OidcVerifier {

    protected abstract val jwksUrl: String
    protected abstract val iss: String
    protected abstract val aud: String

    override fun verify(
        idToken: String,
        nonce: String?,
    ): OidcClaims {
        log.info { "OIDC 토큰 검증 시작: provider=$provider" }
        val kid = extractKid(idToken)
        val publicKey = findPublicKey(kid)
        return extractClaims(idToken, publicKey, nonce)
    }

    // 검증 없이 헤더만 Base64 디코딩해 kid 추출
    private fun extractKid(idToken: String): String {
        return try {
            val header = String(Base64.getUrlDecoder().decode(idToken.split(".")[0]))
            Regex("\"kid\"\\s*:\\s*\"([^\"]+)\"").find(header)?.groupValues?.get(1)
                ?: throw GeneralException(AuthErrorCode.INVALID_TOKEN)
        } catch (e: GeneralException) {
            log.warn { "OIDC 토큰 kid 추출 실패: provider=$provider" }
            throw e
        } catch (e: Exception) {
            log.warn(e) { "OIDC 토큰 헤더 파싱 실패: provider=$provider" }
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
                log.warn { "OIDC 공개키 캐시 미스, 무효화 후 재조회: provider=$provider, kid=$kid" }
                jwksPublicKeyProvider.invalidate(jwksUrl)
                findIn() ?: run {
                    log.warn { "OIDC 공개키를 찾을 수 없음: provider=$provider, kid=$kid" }
                    throw GeneralException(AuthErrorCode.INVALID_TOKEN)
                }
            }
    }

    // iss, aud, exp, 서명 한 번에 검증 후 클레임 추출
    private fun extractClaims(
        idToken: String,
        publicKey: RSAPublicKey,
        nonce: String?,
    ): OidcClaims {
        return try {
            val parserBuilder = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(iss)
                .requireAudience(aud)

            if (nonce != null) {
                parserBuilder.require("nonce", nonce)
            }

            val claims = parserBuilder
                .build()
                .parseClaimsJws(idToken)
                .body

            log.info { "OIDC 토큰 검증 성공: provider=$provider" }

            OidcClaims(
                sub = claims.subject,
                email = claims["email"] as? String,
            )
        } catch (e: ExpiredJwtException) {
            log.warn { "OIDC 토큰 만료: provider=$provider" }
            throw GeneralException(AuthErrorCode.EXPIRED_TOKEN)
        } catch (e: JwtException) {
            log.warn(e) { "OIDC 토큰 검증 실패: provider=$provider" }
            throw GeneralException(AuthErrorCode.INVALID_TOKEN)
        } catch (e: Exception) {
            log.warn(e) { "OIDC 토큰 검증 중 예상치 못한 오류: provider=$provider" }
            throw GeneralException(AuthErrorCode.INVALID_TOKEN)
        }
    }

}
