package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "google")
data class GoogleProperties(
    var iss: String = "https://accounts.google.com",
    var jwksUrl: String = "https://www.googleapis.com/oauth2/v3/certs",
    var webClientId: String = "",
    var androidClientId: String = "",
    var iosClientId: String = "",
)
