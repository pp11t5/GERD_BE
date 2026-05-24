package com.gerd.global.config

import io.sentry.Sentry
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class SentryConfig(
    @Value("\${sentry.dsn:}") private val dsn: String,
    @Value("\${sentry.environment:}") private val environment: String,
    @Value("\${sentry.release:}") private val release: String,
    @Value("\${sentry.traces-sample-rate:0.0}") private val tracesSampleRate: Double,
    @Value("\${sentry.send-default-pii:false}") private val sendDefaultPii: Boolean,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        // DSN이 없으면 로컬 환경으로 간주하고 초기화 건너뜀
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
        }
        log.info("Sentry initialized")
    }
}
