package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    var secret: String = "",
    var accessExpirationMs: Long = 3600000,
    var refreshExpirationMs: Long = 259200000,
)
