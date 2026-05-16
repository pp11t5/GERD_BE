package com.gerd.global.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/** 요청마다 UUID traceId를 생성해 MDC와 응답 헤더에 심고, 요청 종료 후 MDC를 정리 */
@Component
class TraceIdFilter : OncePerRequestFilter() {

    companion object {
        const val TRACE_ID_KEY = "traceId"
        const val TRACE_ID_HEADER = "X-Trace-Id"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // traceId 생성 및 MDC·응답 헤더 세팅
        val traceId = UUID.randomUUID().toString()
        MDC.put(TRACE_ID_KEY, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            // 스레드 재사용 대비 MDC 정리
            MDC.remove(TRACE_ID_KEY)
        }
    }
}
