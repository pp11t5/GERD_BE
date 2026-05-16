package com.gerd.global.security

import com.gerd.domain.auth.entity.User
import com.gerd.global.config.properties.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Base64
import java.util.Date
import kotlin.text.Charsets.UTF_8

@Component
class JwtProvider(private val jwtProperties: JwtProperties) {

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER = "Bearer "
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var key: Key

    @PostConstruct
    fun init() {
        val bytes = decodeSecret(jwtProperties.secret)
        key = Keys.hmacShaKeyFor(bytes)
    }

    fun createAccessToken(user: User): String {
        val now = Date()
        return Jwts.builder()
            .claim("userId", user.id)
            .claim("role", user.role.toString())
            .setIssuedAt(now)
            .setExpiration(Date(now.time + jwtProperties.accessExpirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun createRefreshToken(user: User): String {
        val now = Date()
        return Jwts.builder()
            .claim("userId", user.id)
            .claim("tokenType", "REFRESH")
            .setIssuedAt(now)
            .setExpiration(Date(now.time + jwtProperties.refreshExpirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        try {
            parseJws(token)
            true
        } catch (e: ExpiredJwtException) {
            log.warn("Expired token: {}", e.claims.expiration)
            false
        } catch (e: JwtException) {
            log.error("Invalid token: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            log.error("Invalid token: {}", e.message)
            false
        }

    fun isValidToken(token: String): Boolean = validateToken(token)

    fun isExpiredToken(token: String): Boolean =
        try {
            parseJws(token)
            false
        } catch (_: ExpiredJwtException) {
            true
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }

    fun parseClaims(token: String): Claims =
        try {
            parseJws(token).body
        } catch (e: ExpiredJwtException) {
            e.claims
        }

    fun extractUserId(token: String): Long =
        parseJws(token).body.get("userId", Number::class.java).toLong()

    private fun parseJws(token: String) =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)

    private fun decodeSecret(secret: String): ByteArray =
        runCatching { Base64.getDecoder().decode(secret) }
            .getOrElse { secret.toByteArray(UTF_8) }
}
