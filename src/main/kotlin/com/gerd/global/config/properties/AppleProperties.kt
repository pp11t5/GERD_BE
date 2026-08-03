package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "apple")
data class AppleProperties(
    // Apple ID Token 검증
    var iss: String = "https://appleid.apple.com",
    var jwksUrl: String = "https://appleid.apple.com/auth/keys",

    // OAuth client_id이자 ID Token의 aud
    // iOS 네이티브 앱이면 일반적으로 Bundle ID
    var clientId: String = "",

    // Apple client_secret JWT 생성
    var teamId: String = "",
    var keyId: String = "",
    var privateKey: String = "",

    // Apple REST API
    var tokenUrl: String = "https://appleid.apple.com/auth/token",
    var revokeUrl: String = "https://appleid.apple.com/auth/revoke",
)
