package com.gerd.infra.monitoring.sentry

import io.sentry.Sentry
import io.sentry.SentryOptions.RequestSize
import io.sentry.spring.jakarta.SentryExceptionResolver
import io.sentry.spring.jakarta.SentrySpringFilter
import io.sentry.spring.jakarta.tracing.SpringMvcTransactionNameProvider
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SentryConfig(
    @Value("\${sentry.dsn:}") private val dsn: String,
    @Value("\${sentry.environment:}") private val environment: String,
    @Value("\${sentry.release:}") private val release: String,
    @Value("\${sentry.traces-sample-rate:0.0}") private val tracesSampleRate: Double,
    @Value("\${sentry.send-default-pii:false}") private val sendDefaultPii: Boolean,
    @Value("\${sentry.max-request-body-size:none}") private val maxRequestBodySize: String,
    @Value("\${sentry.exception-resolver-order:-2147483647}") private val exceptionResolverOrder: Int,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        if (dsn.isBlank()) {
            log.info("Sentry DSN not configured, skipping initialization")
            return
        }

        Sentry.init { options ->
            options.dsn = dsn
            if (environment.isNotBlank()) {
                options.environment = environment
            }
            if (release.isNotBlank()) {
                options.release = release
            }
            options.tracesSampleRate = tracesSampleRate
            options.isSendDefaultPii = sendDefaultPii
            options.maxRequestBodySize = parseRequestSize(maxRequestBodySize)
        }
        log.info("Sentry initialized")
    }

    @Bean
    fun sentrySpringFilter(): SentrySpringFilter =
        SentrySpringFilter(Sentry.getCurrentScopes())

    @Bean
    fun sentryExceptionResolver(): SentryExceptionResolver =
        SentryExceptionResolver(
            Sentry.getCurrentScopes(),
            SpringMvcTransactionNameProvider(),
            exceptionResolverOrder,
        )

    private fun parseRequestSize(value: String): RequestSize =
        runCatching { RequestSize.valueOf(value.uppercase()) }.getOrDefault(RequestSize.NONE)
}
