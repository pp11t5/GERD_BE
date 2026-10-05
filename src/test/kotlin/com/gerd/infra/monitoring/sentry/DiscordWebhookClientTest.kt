package com.gerd.infra.monitoring.sentry

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.web.client.RestClient

@ExtendWith(MockitoExtension::class)
class DiscordWebhookClientTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var restClient: RestClient

    private lateinit var client: DiscordWebhookClient
    private val message = DiscordWebhookMessage(embeds = emptyList())

    @BeforeEach
    fun setUp() {
        client = DiscordWebhookClient(restClient)
    }

    @Test
    fun `웹훅 URL이 비어 있으면 요청을 보내지 않는다`() {
        client.send("", message)

        verify(restClient, never()).post()
    }

    @Test
    fun `웹훅 URL이 https가 아니면 요청을 보내지 않는다`() {
        client.send("http://discord.com/api/webhooks/1/2", message)

        verify(restClient, never()).post()
    }

    @Test
    fun `웹훅 URL이 https면 요청을 보낸다`() {
        client.send("https://discord.com/api/webhooks/1/2", message)

        verify(restClient).post()
    }
}
