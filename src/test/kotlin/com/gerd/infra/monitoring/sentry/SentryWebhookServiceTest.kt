package com.gerd.infra.monitoring.sentry

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@ExtendWith(MockitoExtension::class)
class SentryWebhookServiceTest {

    @Mock
    private lateinit var discordWebhookClient: DiscordWebhookClient

    private val properties = SentryWebhookProperties(secret = "webhook-secret")
    private lateinit var service: SentryWebhookService

    @BeforeEach
    fun setUp() {
        service = SentryWebhookService(properties, discordWebhookClient, JsonMapper.builder().build())
    }

    @Nested
    inner class `receive` {

        @Test
        fun `유효한 Sentry 서명이면 Discord 임베드 메시지로 전달한다`() {
            val payload = """
                {
                  "data": {
                    "issue": {
                      "title": "유효하지 않은 Refresh Token입니다.",
                      "project": { "slug": "gerd" }
                    },
                    "event": {
                      "message": "AUTH401_5",
                      "web_url": "https://sentry.io/issues/1",
                      "level": "error",
                      "tags": [["environment", "staging"]]
                    }
                  }
                }
            """.trimIndent().toByteArray()

            val accepted = service.receive(payload, signatureOf(payload))

            assertThat(accepted).isTrue()
            val captor = argumentCaptor<DiscordWebhookMessage>()
            verify(discordWebhookClient).send(captor.capture())
            val embed = captor.firstValue.embeds.single()
            assertThat(embed.title).isEqualTo("🚨 유효하지 않은 Refresh Token입니다.")
            assertThat(embed.description).isEqualTo("AUTH401_5\n\nSentry URL: https://sentry.io/issues/1")
            assertThat(embed.url).isEqualTo("https://sentry.io/issues/1")
            assertThat(embed.fields).containsExactly(
                DiscordEmbedField("Project", "gerd", true),
                DiscordEmbedField("Environment", "staging", true),
                DiscordEmbedField("Level", "error", true),
            )
        }

        @Test
        fun `event의 environment 필드를 Discord 임베드에 표시한다`() {
            val payload = """
                {
                  "data": {
                    "issue": { "title": "서버 오류", "project": { "slug": "gerd" } },
                    "event": { "environment": "production" }
                  }
                }
            """.trimIndent().toByteArray()

            service.receive(payload, signatureOf(payload))

            val captor = argumentCaptor<DiscordWebhookMessage>()
            verify(discordWebhookClient).send(captor.capture())
            assertThat(captor.firstValue.embeds.single().fields)
                .contains(DiscordEmbedField("Environment", "production", true))
        }

        @Test
        fun `서명이 다르면 Discord로 전달하지 않는다`() {
            val payload = "{}".toByteArray()

            val accepted = service.receive(payload, "invalid")

            assertThat(accepted).isFalse()
            verify(discordWebhookClient, never()).send(org.mockito.kotlin.any())
        }
    }

    private fun signatureOf(payload: ByteArray): String =
        Mac.getInstance("HmacSHA256")
            .apply { init(SecretKeySpec(properties.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")) }
            .doFinal(payload)
            .joinToString("") { "%02x".format(it) }
}
