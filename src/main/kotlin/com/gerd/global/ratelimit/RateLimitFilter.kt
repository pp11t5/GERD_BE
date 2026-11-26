package com.gerd.global.ratelimit

import com.gerd.domain.auth.security.SecurityErrorResponseWriter
import com.gerd.global.apiPayload.code.CommonErrorCode
import com.gerd.global.config.properties.RateLimitProperties
import com.gerd.global.config.properties.RateLimitProperties.KeyType
import com.gerd.global.config.properties.RateLimitProperties.Policy
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UrlPathHelper

class RateLimitFilter(
    private val properties: RateLimitProperties,
    private val buckets: RateLimitBuckets,
    private val clientIpResolver: ClientIpResolver,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val pathMatcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val policy = findPolicy(UrlPathHelper.defaultInstance.getPathWithinApplication(request))
        if (!properties.enabled || policy == null) {
            filterChain.doFilter(request, response)
            return
        }

        val ip = clientIpResolver.resolve(request)
        val result = buckets.tryConsume(policy, resolveKey(policy, ip))
        if (result.allowed) {
            filterChain.doFilter(request, response)
            return
        }

        log.warn("rate limit exceeded: policy={}, ip={}", policy.name, maskIp(ip))
        response.setHeader(HttpHeaders.RETRY_AFTER, result.retryAfterSeconds.toString())
        SecurityErrorResponseWriter.write(response, CommonErrorCode.TOO_MANY_REQUESTS)
    }

    private fun findPolicy(uri: String): Policy? =
        properties.policies.firstOrNull { policy ->
            policy.pathPatterns.any { pathMatcher.match(it, uri) }
        }

    private fun resolveKey(
        policy: Policy,
        ip: String,
    ): String {
        if (policy.keyType == KeyType.USER) {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null && authentication.isAuthenticated && authentication !is AnonymousAuthenticationToken) {
                return "user:${authentication.name}"
            }
        }
        return "ip:$ip"
    }

    private fun maskIp(ip: String): String {
        if (ip.contains(':')) {
            return ip.split(":").take(IPV6_VISIBLE_GROUPS).joinToString(":") + ":*"
        }
        return ip.substringBeforeLast('.', ip) + ".*"
    }

    companion object {
        private const val IPV6_VISIBLE_GROUPS = 3
    }
}
