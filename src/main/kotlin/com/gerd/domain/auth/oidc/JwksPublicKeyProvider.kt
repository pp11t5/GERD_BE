package com.gerd.domain.auth.oidc

import com.gerd.domain.auth.client.JwksClient
import com.gerd.domain.auth.dto.JwksResponseDTO
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 프로바이더별 JWKS 공개키를 Caffeine 캐시로 관리
 * 매번 공개키를 요청하게 되면 카카오 측에서 요청을 막을 수가 있으므로 캐싱으로 처리
 */
@Component
class JwksPublicKeyProvider(private val jwksClient: JwksClient) {

    // JWKS URL을 캐시 키로 사용 — 프로바이더별 공개키 1일 캐싱
    private val cache: Cache<String, JwksResponseDTO> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build()

    // 캐시에서 공개키 조회, 없으면 JWKS 엔드포인트에서 가져와 캐시에 저장
    fun getKeys(jwksUrl: String): JwksResponseDTO? =
        cache.get(jwksUrl) { jwksClient.getPublicKeys(it) }

    // stale 캐시 대비 강제 무효화
    fun invalidate(jwksUrl: String) = cache.invalidate(jwksUrl)
}
