package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "apple")
data class AppleProperties(
    var iss: String = "",
    var jwksUrl: String = "",
    var bundleId: String = "",
)
