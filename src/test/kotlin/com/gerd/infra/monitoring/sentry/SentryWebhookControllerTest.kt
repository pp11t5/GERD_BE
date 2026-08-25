package com.gerd.infra.monitoring.sentry

import com.gerd.domain.auth.security.AccessTokenBlacklist
import com.gerd.domain.auth.security.JwtProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.mockito.kotlin.whenever

@WebMvcTest(controllers = [SentryWebhookController::class])
@AutoConfigureMockMvc(addFilters = false)
class SentryWebhookControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var sentryWebhookService: SentryWebhookService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var accessTokenBlacklist: AccessTokenBlacklist

    @Test
    fun `유효한 webhook은 204를 반환한다`() {
        whenever(sentryWebhookService.receive("{}".toByteArray(), "signature")).thenReturn(true)

        mockMvc.post("/api/v1/webhooks/sentry") {
            header("Sentry-Hook-Signature", "signature")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `서명이 유효하지 않으면 401을 반환한다`() {
        whenever(sentryWebhookService.receive("{}".toByteArray(), null)).thenReturn(false)

        mockMvc.post("/api/v1/webhooks/sentry") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
