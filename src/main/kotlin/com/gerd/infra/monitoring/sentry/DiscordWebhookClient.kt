package com.gerd.infra.monitoring.sentry

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

private val log = KotlinLogging.logger {}

@Component
class DiscordWebhookClient(
    private val sentryWebhookProperties: SentryWebhookProperties,
    private val restClient: RestClient,
) {

    fun send(message: DiscordWebhookMessage) {
        if (sentryWebhookProperties.discordWebhookUrl.isBlank()) {
            log.warn { "Discord webhook URL is not configured, skipping Sentry alert" }
            return
        }

        try {
            restClient.post()
                .uri(sentryWebhookProperties.discordWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(message)
                .retrieve()
                .toBodilessEntity()
        } catch (exception: RestClientException) {
            log.error(exception) { "Failed to deliver Sentry alert to Discord" }
        }
    }
}

data class DiscordWebhookMessage(
    val embeds: List<DiscordEmbed>,
)

data class DiscordEmbed(
    val title: String,
    val description: String,
    val url: String?,
    val color: Int,
    val fields: List<DiscordEmbedField>,
)

data class DiscordEmbedField(
    val name: String,
    val value: String,
    val inline: Boolean,
)
