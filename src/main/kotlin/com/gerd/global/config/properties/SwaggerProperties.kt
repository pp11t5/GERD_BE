package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "swagger")
data class SwaggerProperties(
    var username: String = "admin",
    var password: String = "admin",
)
