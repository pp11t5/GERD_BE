package com.gerd.global.ratelimit

import com.gerd.global.config.properties.RateLimitProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class ClientIpResolver(
    private val properties: RateLimitProperties,
) {
    fun resolve(request: HttpServletRequest): String {
        val header = properties.clientIpHeader
        if (header.isNotBlank()) {
            // 프록시가 뒤에 덧붙이는 값이라 마지막 값만 신뢰, 앞쪽은 클라이언트가 위조 가능
            val values = request.getHeader(header).orEmpty().split(",")
            val forwarded = values.last().trim()
            if (forwarded.isNotEmpty()) {
                return forwarded
            }
        }
        return request.remoteAddr
    }
}
