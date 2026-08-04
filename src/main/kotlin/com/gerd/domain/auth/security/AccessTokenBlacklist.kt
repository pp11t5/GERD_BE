package com.gerd.domain.auth.security

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.gerd.global.config.properties.JwtProperties
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 탈퇴 등으로 즉시 무효화해야 하는 액세스 토큰을 jti 기준으로 관리
 * 액세스 토큰은 stateless라 만료 전까지는 그대로 유효하므로 블랙리스트로 보완
 */
@Component
class AccessTokenBlacklist(
    jwtProperties: JwtProperties,
) {
    // 등록 시점 기준 액세스 토큰 최대 수명만큼만 보관 — 그 이후엔 토큰 자체가 만료되어 무의미
    private val cache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(jwtProperties.accessExpirationMs, TimeUnit.MILLISECONDS)
        .build()

    fun blacklist(jti: String) {
        cache.put(jti, true)
    }

    fun isBlacklisted(jti: String): Boolean =
        cache.getIfPresent(jti) != null
}
