package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "google")
data class GoogleProperties(
    var iss: String = "https://accounts.google.com",
    var jwksUrl: String = "https://www.googleapis.com/oauth2/v3/certs",
    // 앱이 requestIdToken/serverClientID에 Web Client ID를 넘기면 발급된 ID Token의 aud도 Web Client ID가 된다
    // (Android/iOS Client ID는 네이티브 로그인 화면의 패키지명·SHA1/번들ID 검증용이지 ID Token의 aud가 아니다)
    var webClientId: String = "",
    var androidClientId: String = "",
    var iosClientId: String = "",
)
