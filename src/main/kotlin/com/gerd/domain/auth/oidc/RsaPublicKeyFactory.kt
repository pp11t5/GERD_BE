package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.apiPayload.GeneralException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

/**
 * JWKS 키 파라미터(n, e)로 RSA 공개키 조립
 */
object RsaPublicKeyFactory {

    // n, e 값으로 RSA 공개키 생성
    fun create(n: String, e: String): RSAPublicKey {
        val modulus = BigInteger(1, Base64.getUrlDecoder().decode(n))
        val exponent = BigInteger(1, Base64.getUrlDecoder().decode(e))
        return try {
            KeyFactory.getInstance("RSA")
                .generatePublic(RSAPublicKeySpec(modulus, exponent)) as RSAPublicKey
        } catch (ex: Exception) {
            throw GeneralException(AuthErrorCode.INVALID_TOKEN)
        }
    }
}
