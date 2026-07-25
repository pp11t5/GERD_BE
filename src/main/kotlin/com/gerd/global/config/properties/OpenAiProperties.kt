package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "openai")
data class OpenAiProperties(
    var apiKey: String = "",
    var model: String = "gpt-4o-mini",
    var baseUrl: String = "https://api.openai.com",
    var connectTimeoutMs: Long = 3000,
    var readTimeoutMs: Long = 15000,
)
