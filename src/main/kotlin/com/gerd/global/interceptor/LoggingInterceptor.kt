package com.gerd.global.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val log = LoggerFactory.getLogger(LoggingInterceptor::class.java)

/** 요청 진입·완료 시점에 메서드, URI, 상태코드, 소요시간, traceId를 로깅*/
@Component
class LoggingInterceptor : HandlerInterceptor {

    companion object {
        private const val START_TIME_ATTR = "requestStartTime"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // 소요시간 계산용 시작 시각 저장
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis())
        log.info("→ {} {} (traceId={})", request.method, request.requestURI, MDC.get("traceId"))
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val duration = System.currentTimeMillis() - (request.getAttribute(START_TIME_ATTR) as? Long ?: 0L)
        log.info(
            "← {} {} {} {}ms (traceId={})",
            request.method, request.requestURI, response.status, duration, MDC.get("traceId"),
        )
    }
}
