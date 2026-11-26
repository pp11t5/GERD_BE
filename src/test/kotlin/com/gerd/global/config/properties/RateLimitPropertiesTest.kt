package com.gerd.global.config.properties

import com.gerd.global.config.properties.RateLimitProperties.Policy
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

class RateLimitPropertiesTest {
    private fun properties(
        vararg policies: Policy,
        enabled: Boolean = true,
    ) = RateLimitProperties(enabled = enabled, policies = policies.toList())

    @Nested
    inner class `validate` {
        @Test
        fun `올바른 정책은 통과한다`() {
            assertThatCode { properties(Policy(name = "a")).validate() }.doesNotThrowAnyException()
        }

        @Test
        fun `한도가 0이면 시작 시 실패한다`() {
            assertThatThrownBy { properties(Policy(name = "a", limitForPeriod = 0)).validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("limit-for-period")
        }

        @Test
        fun `주기가 0이면 시작 시 실패한다`() {
            assertThatThrownBy { properties(Policy(name = "a", refreshPeriod = Duration.ZERO)).validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("refresh-period")
        }

        @Test
        fun `정책 이름이 중복되면 시작 시 실패한다`() {
            assertThatThrownBy { properties(Policy(name = "a"), Policy(name = "a")).validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("중복")
        }

        @Test
        fun `IP 헤더를 지정하고 신뢰 프록시가 없으면 시작 시 실패한다`() {
            val properties = RateLimitProperties(clientIpHeader = "CF-Connecting-IP")

            assertThatThrownBy { properties.validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("trusted-proxies")
        }

        @Test
        fun `IP 헤더와 신뢰 프록시를 함께 지정하면 통과한다`() {
            val properties =
                RateLimitProperties(clientIpHeader = "CF-Connecting-IP", trustedProxies = listOf("10.0.0.0/8"))

            assertThatCode { properties.validate() }.doesNotThrowAnyException()
        }

        @Test
        fun `비활성화되어 있으면 잘못된 값도 검사하지 않는다`() {
            assertThatCode { properties(Policy(name = "a", limitForPeriod = 0), enabled = false).validate() }
                .doesNotThrowAnyException()
        }
    }
}
