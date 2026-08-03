package com.gerd.domain.auth.util

import com.gerd.global.config.properties.AppleProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.Date
import kotlin.io.encoding.Base64

/**
 * Client secret을 생성하는 Util
 */
@Component
class AppleLoginUtil(
    private val appleProperties: AppleProperties,
) {
    private val privateKey: ECPrivateKey by lazy {
        parsePrivateKey(appleProperties.privateKey)
    }

    fun generateClientSecret(): String {
        val now = Instant.now()

        return Jwts.builder()
            .setHeaderParam("kid", appleProperties.keyId)
            .setIssuer(appleProperties.teamId)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(CLIENT_SECRET_VALIDITY)))
            .setAudience(appleProperties.iss)
            .setSubject(appleProperties.clientId)
            .signWith(privateKey, SignatureAlgorithm.ES256)
            .compact()
    }

    // .p8 파일 내용을 ECPrivateKey 타입으로 변환
    private fun parsePrivateKey(privateKeyBase64: String): ECPrivateKey {
        // base64 값 디코딩
        val pem = Base64.decode(privateKeyBase64).toString(Charsets.UTF_8)
        // pem header 제거 후 본문만 추출
        val privateKeyPem = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace(Regex("\\s+"), "")
        // base64를 바이너리로 디코딩
        val keyBytes = Base64.decode(privateKeyPem)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECPrivateKey
    }
    companion object {
        private val CLIENT_SECRET_VALIDITY = Duration.ofHours(1)
    }
}
