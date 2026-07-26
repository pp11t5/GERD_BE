package com.gerd.global.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(1)
class LoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return uri.startsWith("/health") ||
            uri.startsWith("/swagger-ui") ||
            uri.startsWith("/v3/api-docs") ||
            uri.startsWith("/webjars") ||
            uri.startsWith("/css") ||
            uri.startsWith("/js") ||
            uri.startsWith("/images") ||
            uri.startsWith("/static") ||
            uri == "/favicon.ico" ||
            uri == "/error"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val start = System.currentTimeMillis()
        log.info("→ {} {} (traceId={})", request.method, request.requestURI, MDC.get("traceId"))
        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - start
            val status = response.status
            val msg = "← {} {} {} {}ms (traceId={})"
            val args = arrayOf(request.method, request.requestURI, status, duration, MDC.get("traceId"))
            when {
                status >= 500 -> log.error(msg, *args)
                status >= 400 -> log.warn(msg, *args)
                else -> log.info(msg, *args)
            }
        }
    }
}
