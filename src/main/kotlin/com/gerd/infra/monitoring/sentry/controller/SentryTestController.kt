package com.gerd.infra.monitoring.sentry.controller

import com.gerd.global.apiPayload.ApiResponse
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("staging")
class SentryTestController : SentryTestApi {

    override fun throwSentryTestError(): ApiResponse<Unit> {
        throw IllegalStateException("Intentional Sentry test error")
    }
}
