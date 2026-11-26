package com.gerd.global.ratelimit

import com.gerd.global.config.properties.RateLimitProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Component

@Component
class ClientIpResolver(
    private val properties: RateLimitProperties,
) {
    private val trustedMatchers: List<IpAddressMatcher> =
        if (properties.enabled && properties.clientIpHeader.isNotBlank()) {
            properties.trustedProxies.map { IpAddressMatcher(it.trim()) }
        } else {
            emptyList()
        }

    fun resolve(request: HttpServletRequest): String {
        val header = properties.clientIpHeader
        if (header.isNotBlank() && isTrustedPeer(request.remoteAddr)) {
            // 프록시가 뒤에 덧붙이는 값이라 마지막 값만 신뢰, 앞쪽은 클라이언트가 위조 가능
            val values = request.getHeader(header).orEmpty().split(",")
            val forwarded = values.last().trim()
            if (forwarded.isNotEmpty()) {
                return forwarded
            }
        }
        return request.remoteAddr
    }

    // 신뢰 프록시 목록이 있으면 접속한 상대가 그 목록에 있을 때만 헤더를 믿음, 비어 있으면 모든 상대를 신뢰
    private fun isTrustedPeer(peer: String): Boolean {
        if (trustedMatchers.isEmpty()) {
            return true
        }
        return trustedMatchers.any { it.matches(peer) }
    }
}
