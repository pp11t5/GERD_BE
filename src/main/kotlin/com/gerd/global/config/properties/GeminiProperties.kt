package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties(
    var apiKey: String = "",
    var model: String = "gemini-2.5-flash-lite",
    var baseUrl: String = "https://generativelanguage.googleapis.com",
    var connectTimeoutMs: Long = 3000,
    var readTimeoutMs: Long = 15000,
)
