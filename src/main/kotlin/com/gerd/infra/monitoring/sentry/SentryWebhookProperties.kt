package com.gerd.infra.monitoring.sentry

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "sentry.webhook")
data class SentryWebhookProperties(
    var secret: String = "",
    var discordWebhookUrl: String = "",
)
