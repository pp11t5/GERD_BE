package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(
    var serviceAccountJson: String = "",
    var projectId: String = "",
)
