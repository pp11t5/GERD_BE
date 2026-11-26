package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    var enabled: Boolean = true,
    var clientIpHeader: String = "",
    var policies: List<Policy> = emptyList(),
) {
    data class Policy(
        var name: String = "",
        var pathPatterns: List<String> = emptyList(),
        var keyType: KeyType = KeyType.IP,
        var limitForPeriod: Int = 10,
        var refreshPeriod: Duration = Duration.ofMinutes(1),
    )

    enum class KeyType { IP, USER }
}
