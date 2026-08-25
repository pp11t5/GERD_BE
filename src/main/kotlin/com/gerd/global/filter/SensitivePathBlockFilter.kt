package com.gerd.global.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter

class SensitivePathBlockFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (isSensitivePath(request.requestURI)) {
            response.sendError(HttpStatus.NOT_FOUND.value())
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isSensitivePath(uri: String): Boolean {
        val path = uri.lowercase()
        val segments = path.split('/').filter(String::isNotBlank)

        return segments.any { it.startsWith('.') } ||
            path in BLOCKED_PATHS ||
            path.endsWith(".php")
    }

    companion object {
        private val BLOCKED_PATHS = setOf(
            "/wp-admin/install.php",
            "/wp-login.php",
            "/aws-ses.json",
            "/config.json.save",
        )
    }
}
