package com.gerd.infra.monitoring.sentry

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
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
        val projectName = project.path("slug").asText(project.path("name").asText("unknown"))
        val level = event.path("level").asText("error")
        val environment = extractEnvironment(event)
        val culprit = issue.path("culprit").asText(event.path("culprit").asText(""))
        val fields = mutableListOf(
            DiscordEmbedField(name = "Project", value = projectName, inline = true),
            DiscordEmbedField(name = "Environment", value = environment, inline = true),
            DiscordEmbedField(name = "Level", value = level, inline = true),
        )
        extractRelease(event)?.let { fields += DiscordEmbedField(name = "Release", value = it, inline = true) }
        if (culprit.isNotBlank()) {
            fields += DiscordEmbedField(name = "Culprit", value = "`${culprit.take(MAX_CULPRIT_LENGTH)}`", inline = false)
        }
        sentryWebhookProperties.runbookUrl.takeIf(String::isNotBlank)?.let {
            fields += DiscordEmbedField(name = "Runbook", value = "[런북 열기]($it)", inline = false)
        }

        discordWebhookClient.send(
            sentryWebhookProperties.discordWebhookUrl,
            DiscordWebhookMessage(
                embeds = listOf(
                    DiscordEmbed(
                        title = "${levelEmoji(level)} $title".take(MAX_TITLE_LENGTH),
                        description = description.take(MAX_DESCRIPTION_LENGTH),
                        url = issueUrl,
                        color = levelColor(level),
                        fields = fields,
                        timestamp = parseTimestamp(event.path("datetime").asText(null)),
                        footer = DiscordEmbedFooter(text = "Sentry"),
                    ),
                ),
            ),
        )
        return true
    }

    private fun levelEmoji(level: String): String = when (level.lowercase()) {
        "fatal" -> "💀"
        "warning" -> "⚠️"
        "info", "debug" -> "ℹ️"
        else -> "🚨"
    }

    private fun levelColor(level: String): Int = when (level.lowercase()) {
        "fatal" -> FATAL_COLOR
        "warning" -> WARNING_COLOR
        "info", "debug" -> INFO_COLOR
        else -> ERROR_COLOR
    }

    private fun extractRelease(event: JsonNode): String? =
        event.path("release").takeIf(JsonNode::isString)
            ?.asText()
            ?.takeIf(String::isNotBlank)
            ?.let { if (it.length == GIT_SHA_LENGTH && it.all { c -> c in HEX_DIGITS }) it.take(SHORT_SHA_LENGTH) else it }
            ?.take(MAX_RELEASE_LENGTH)

    private fun parseTimestamp(value: String?): String? =
        value?.let { runCatching { Instant.parse(it).toString() }.getOrNull() }

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
        private const val MAX_TITLE_LENGTH = 256
        private const val MAX_DESCRIPTION_LENGTH = 4_000
        private const val MAX_CULPRIT_LENGTH = 900
        private const val MAX_RELEASE_LENGTH = 100
        private const val GIT_SHA_LENGTH = 40
        private const val SHORT_SHA_LENGTH = 7
        private const val ENVIRONMENT_TAG = "environment"
        private const val UNKNOWN_ENVIRONMENT = "unknown"
        private const val ERROR_COLOR = 15_158_332
        private const val FATAL_COLOR = 10_036_732
        private const val WARNING_COLOR = 15_965_202
        private const val INFO_COLOR = 3_447_003
    }
}
