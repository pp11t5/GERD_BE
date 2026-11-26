package com.gerd.global.ratelimit

import com.gerd.global.config.properties.RateLimitProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ClientIpResolverTest {
    private fun resolver(header: String) = ClientIpResolver(RateLimitProperties(clientIpHeader = header))

    private fun request() = MockHttpServletRequest().apply { remoteAddr = "10.0.0.1" }

    @Nested
    inner class `resolve` {
        @Test
        fun `헤더를 설정하지 않으면 remoteAddr를 쓴다`() {
            val request = request()
            request.addHeader("X-Forwarded-For", "9.9.9.9")

            assertThat(resolver("").resolve(request)).isEqualTo("10.0.0.1")
        }

        @Test
        fun `헤더 값이 하나면 그 값을 쓴다`() {
            val request = request()
            request.addHeader("CF-Connecting-IP", "1.2.3.4")

            assertThat(resolver("CF-Connecting-IP").resolve(request)).isEqualTo("1.2.3.4")
        }

        @Test
        fun `쉼표로 이어진 값은 마지막 값만 쓴다`() {
            val request = request()
            request.addHeader("X-Forwarded-For", "6.6.6.6, 1.2.3.4")

            assertThat(resolver("X-Forwarded-For").resolve(request)).isEqualTo("1.2.3.4")
        }

        @Test
        fun `설정한 헤더가 요청에 없으면 remoteAddr를 쓴다`() {
            assertThat(resolver("CF-Connecting-IP").resolve(request())).isEqualTo("10.0.0.1")
        }
    }
}
