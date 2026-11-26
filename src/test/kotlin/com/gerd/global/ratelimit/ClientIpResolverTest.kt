package com.gerd.global.ratelimit

import com.gerd.global.config.properties.RateLimitProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ClientIpResolverTest {
    private fun resolver(
        header: String,
        trustedProxies: List<String> = emptyList(),
    ) = ClientIpResolver(RateLimitProperties(clientIpHeader = header, trustedProxies = trustedProxies))

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
        fun `접속한 상대가 신뢰 프록시면 헤더를 쓴다`() {
            val request = request()
            request.addHeader("CF-Connecting-IP", "1.2.3.4")

            val resolver = resolver("CF-Connecting-IP", listOf("10.0.0.0/24"))

            assertThat(resolver.resolve(request)).isEqualTo("1.2.3.4")
        }

        @Test
        fun `접속한 상대가 신뢰 프록시가 아니면 헤더를 무시하고 remoteAddr를 쓴다`() {
            val request = request()
            request.addHeader("CF-Connecting-IP", "6.6.6.6")

            val resolver = resolver("CF-Connecting-IP", listOf("192.168.0.0/16"))

            assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1")
        }

        @Test
        fun `신뢰 프록시 형식이 잘못되면 생성 시 실패한다`() {
            assertThatThrownBy { resolver("CF-Connecting-IP", listOf("10.0.0.0/33")) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `비활성화되어 있으면 잘못된 신뢰 프록시도 검사하지 않는다`() {
            val properties =
                RateLimitProperties(enabled = false, clientIpHeader = "CF-Connecting-IP", trustedProxies = listOf("bad"))

            assertThatCode { ClientIpResolver(properties) }.doesNotThrowAnyException()
        }

        @Test
        fun `설정한 헤더가 요청에 없으면 remoteAddr를 쓴다`() {
            assertThat(resolver("CF-Connecting-IP").resolve(request())).isEqualTo("10.0.0.1")
        }
    }
}
