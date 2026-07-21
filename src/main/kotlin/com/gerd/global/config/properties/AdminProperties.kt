package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "admin")
data class AdminProperties(
    var email: String = "",
    var secret: String = "",
)
