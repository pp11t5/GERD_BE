package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "kakao")
data class KakaoProperties(
    var iss: String = "",
    var jwksUrl: String = "",
    var nativeAppKey: String = "",
    var adminKey: String = "",
)
