package com.gerd.infra.monitoring.sentry

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

@Service
class SentryWebhookService(
    private val sentryWebhookProperties: SentryWebhookProperties,
    private val discordWebhookClient: DiscordWebhookClient,
    private val objectMapper: ObjectMapper,
) {

    fun receive(payload: ByteArray, signature: String?): Boolean {
        if (!isValidSignature(payload, signature)) {
            log.warn { "Rejected Sentry webhook with an invalid signature" }
            return false
        }

        val root = runCatching { objectMapper.readTree(payload) }
            .getOrElse {
                log.warn(it) { "Rejected Sentry webhook with an invalid JSON payload" }
                return true
            }
        val data = root.path("data")
        val issue = data.path("issue")
        val event = data.path("event")
        val project = issue.path("project").takeIf { !it.isMissingNode }
            ?: data.path("project")
        val title = issue.path("title").asText("Sentry Alert")
        val description = event.path("message").asText(issue.path("culprit").asText(title))
        val issueUrl = issue.path("web_url").asText(null)
            ?: issue.path("permalink").asText(null)
            ?: event.path("web_url").asText(null)
        val projectName = project.path("slug").asText(project.path("name").asText("unknown"))
        val level = event.path("level").asText("error")
        val environment = extractEnvironment(event)

        discordWebhookClient.send(
            DiscordWebhookMessage(
                embeds = listOf(
                    DiscordEmbed(
                        title = "🚨 $title",
                        description = descriptionWithUrl(description, issueUrl),
                        url = issueUrl,
                        color = if (level.equals("fatal", ignoreCase = true)) FATAL_COLOR else ERROR_COLOR,
                        fields = listOf(
                            DiscordEmbedField(name = "Project", value = projectName, inline = true),
                            DiscordEmbedField(name = "Environment", value = environment, inline = true),
                            DiscordEmbedField(name = "Level", value = level, inline = true),
                        ),
                    ),
                ),
            ),
        )
        return true
    }

    private fun extractEnvironment(event: JsonNode): String {
        event.path("environment").asText(null)?.takeIf(String::isNotBlank)?.let { return it }

        val tags = event.path("tags")
        tags.path("environment").asText(null)?.takeIf(String::isNotBlank)?.let { return it }

        tags.firstOrNull { tag ->
            tag.path("key").asText() == ENVIRONMENT_TAG || tag.path(0).asText() == ENVIRONMENT_TAG
        }?.let { tag ->
            tag.path("value").asText(null)?.takeIf(String::isNotBlank)
                ?: tag.path(1).asText(null)?.takeIf(String::isNotBlank)
        }?.let { return it }

        return UNKNOWN_ENVIRONMENT
    }

    private fun descriptionWithUrl(description: String, issueUrl: String?): String {
        if (issueUrl.isNullOrBlank()) return description.take(MAX_DESCRIPTION_LENGTH)

        val urlSuffix = "\n\nSentry URL: $issueUrl"
        return description.take((MAX_DESCRIPTION_LENGTH - urlSuffix.length).coerceAtLeast(0)) + urlSuffix
    }

    private fun isValidSignature(payload: ByteArray, signature: String?): Boolean {
        if (sentryWebhookProperties.secret.isBlank() || signature.isNullOrBlank()) return false

        val expectedSignature = Mac.getInstance(HMAC_SHA256)
            .apply {
                init(SecretKeySpec(sentryWebhookProperties.secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256))
            }
            .doFinal(payload)

        return MessageDigest.isEqual(expectedSignature, signature.hexToBytesOrNull())
    }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length != SHA256_HEX_LENGTH || any { it !in HEX_DIGITS }) return null
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
        private const val SHA256_HEX_LENGTH = 64
        private const val HEX_DIGITS = "0123456789abcdefABCDEF"
        private const val MAX_DESCRIPTION_LENGTH = 4_000
        private const val ENVIRONMENT_TAG = "environment"
        private const val UNKNOWN_ENVIRONMENT = "unknown"
        private const val ERROR_COLOR = 15_158_332
        private const val FATAL_COLOR = 10_036_732
    }
}
