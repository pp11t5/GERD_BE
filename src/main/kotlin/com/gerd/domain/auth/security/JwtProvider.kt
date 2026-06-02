package com.gerd.domain.auth.security

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.entity.User
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.text.Charsets.UTF_8

@Component
class JwtProvider(private val jwtProperties: JwtProperties) {

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER = "Bearer "
        private const val TOKEN_TYPE_ACCESS = "ACCESS"
        private const val TOKEN_TYPE_REFRESH = "REFRESH"
    }

    data class JwtToken(val value: String, val jti: String)

    private lateinit var key: Key

    @PostConstruct
    fun init() {
        val bytes = runCatching { Base64.getDecoder().decode(jwtProperties.secret) }
            .getOrElse { jwtProperties.secret.toByteArray(UTF_8) }
        key = Keys.hmacShaKeyFor(bytes)
    }

    // sub = userId, tokenType = ACCESS, email, nickname, role 포함
    fun createAccessToken(user: User): String {
        val now = Date()
        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("tokenType", TOKEN_TYPE_ACCESS)
            .claim("email", user.email)
            .claim("nickname", user.nickname)
            .claim("role", user.role.name)
            .setId(UUID.randomUUID().toString())
            .setIssuedAt(now)
            .setExpiration(Date(now.time + jwtProperties.accessExpirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    // sub = userId, tokenType = REFRESH, jti = Redis 키
    fun createRefreshToken(user: User): JwtToken {
        val now = Date()
        val jti = UUID.randomUUID().toString()
        val value = Jwts.builder()
            .setSubject(user.id.toString())
            .claim("tokenType", TOKEN_TYPE_REFRESH)
            .setId(jti)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + jwtProperties.refreshExpirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
        return JwtToken(value, jti)
    }

    // 서명·만료 검증은 JwtException으로 올려보냄 — 비즈니스 예외 변환은 Filter 책임
    fun validateAccessToken(token: String): Claims =
        validateAndExtract(token, TOKEN_TYPE_ACCESS)

    fun validateRefreshToken(token: String): Claims =
        validateAndExtract(token, TOKEN_TYPE_REFRESH)

    fun extractUserId(claims: Claims): Long =
        claims.subject.toLong()

    fun extractJti(claims: Claims): String =
        claims.id

    fun extractRemainingTtlMs(claims: Claims): Long =
        maxOf(0L, claims.expiration.time - System.currentTimeMillis())

    private fun validateAndExtract(token: String, expectedType: String): Claims {
        val claims = parseJws(token).body  // ExpiredJwtException, JwtException 그대로 throw

        // tokenType 불일치는 잘못된 토큰 사용이므로 공통 인증 예외로 변환
        if (claims["tokenType"] != expectedType) {
            throw GeneralException(AuthErrorCode.INVALID_TOKEN)
        }

        return claims
    }

    private fun parseJws(token: String) =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
}
