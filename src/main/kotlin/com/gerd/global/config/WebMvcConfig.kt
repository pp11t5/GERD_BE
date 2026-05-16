package com.gerd.global.config

import com.gerd.global.interceptor.LoggingInterceptor
import com.gerd.global.resolver.CurrentUserArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val loggingInterceptor: LoggingInterceptor,
    private val currentUserArgumentResolver: CurrentUserArgumentResolver,
) : WebMvcConfigurer {

    companion object {
        private val LOGGING_EXCLUDE_PATTERNS = arrayOf(
            "/health",
            "/health/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/favicon.ico",
            "/error",
            "/webjars/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/static/**",
        )
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggingInterceptor)
            .excludePathPatterns(*LOGGING_EXCLUDE_PATTERNS)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserArgumentResolver)
    }
}
