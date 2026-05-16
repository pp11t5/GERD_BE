package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "r2")
data class R2Properties(
    var credentials: Credentials = Credentials(),
    var endpoint: String = "",
    var bucket: String = "",
) {
    data class Credentials(
        var accessKey: String = "",
        var secretKey: String = "",
    )
}
