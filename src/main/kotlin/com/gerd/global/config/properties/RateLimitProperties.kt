package com.gerd.global.config.properties

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    var enabled: Boolean = true,
    var clientIpHeader: String = "",
    var trustedProxies: List<String> = emptyList(),
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

    @PostConstruct
    fun validate() {
        if (!enabled) {
            return
        }
        check(clientIpHeader.isBlank() || trustedProxies.isNotEmpty()) {
            "rate-limit client-ip-header를 설정하면 trusted-proxies도 설정해야 합니다: $clientIpHeader"
        }
        val duplicated = policies.groupBy { it.name }.filter { it.value.size > 1 }.keys
        check(duplicated.isEmpty()) { "rate-limit 정책 이름이 중복되었습니다: $duplicated" }
        policies.forEach { policy ->
            check(policy.name.isNotBlank()) { "rate-limit 정책 이름이 비어 있습니다" }
            check(policy.limitForPeriod > 0) {
                "rate-limit 정책 '${policy.name}'의 limit-for-period는 1 이상이어야 합니다: ${policy.limitForPeriod}"
            }
            check(!policy.refreshPeriod.isZero && !policy.refreshPeriod.isNegative) {
                "rate-limit 정책 '${policy.name}'의 refresh-period는 0보다 커야 합니다: ${policy.refreshPeriod}"
            }
        }
    }
}
