package com.gerd.infra.monitoring.sentry

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/webhooks")
class SentryWebhookController(
    private val sentryWebhookService: SentryWebhookService,
) {

    @PostMapping("/sentry")
    fun receive(
        @RequestHeader("Sentry-Hook-Signature", required = false) signature: String?,
        @RequestBody payload: ByteArray,
    ): ResponseEntity<Unit> {
        if (!sentryWebhookService.receive(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return ResponseEntity.noContent().build()
    }
}
