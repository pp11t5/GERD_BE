package com.gerd.global.ratelimit

import com.gerd.global.config.properties.RateLimitProperties
import com.gerd.global.config.properties.RateLimitProperties.KeyType
import com.gerd.global.config.properties.RateLimitProperties.Policy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Duration

class RateLimitFilterTest {
    private val adminLogin = "/api/v1/auth/admin/login"

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private fun policy(keyType: KeyType = KeyType.IP): Policy =
        Policy(
            name = "admin-login",
            pathPatterns = listOf(adminLogin),
            keyType = keyType,
            limitForPeriod = 2,
            refreshPeriod = Duration.ofMinutes(10),
        )

    private fun filter(
        policy: Policy = policy(),
        enabled: Boolean = true,
        header: String = "",
    ): RateLimitFilter {
        val properties = RateLimitProperties(enabled = enabled, clientIpHeader = header, policies = listOf(policy))
        return RateLimitFilter(properties, RateLimitBuckets(), ClientIpResolver(properties))
    }

    private class Call(
        val response: MockHttpServletResponse,
        val chain: MockFilterChain,
    )

    private fun RateLimitFilter.call(
        uri: String = adminLogin,
        ip: String = "1.2.3.4",
        headers: Map<String, String> = emptyMap(),
    ): Call {
        val request = MockHttpServletRequest("POST", uri)
        request.remoteAddr = ip
        headers.forEach { (name, value) -> request.addHeader(name, value) }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        doFilter(request, response, chain)
        return Call(response, chain)
    }

    private fun Call.passed() = chain.request != null

    @Nested
    inner class `doFilter` {
        @Test
        fun `한도 안의 요청은 통과한다`() {
            val filter = filter()

            val call = filter.call()

            assertThat(call.passed()).isTrue()
            assertThat(call.response.status).isEqualTo(200)
        }

        @Test
        fun `한도를 넘으면 429와 Retry-After를 응답하고 체인을 끊는다`() {
            val filter = filter()
            filter.call()
            filter.call()

            val call = filter.call()

            assertThat(call.passed()).isFalse()
            assertThat(call.response.status).isEqualTo(429)
            assertThat(call.response.getHeader("Retry-After")).isNotNull()
            assertThat(call.response.contentAsString).contains("COMMON429_1")
        }

        @Test
        fun `퍼센트 인코딩한 경로로도 정책을 피할 수 없다`() {
            val filter = filter()
            val encoded = "/api/v1/auth/admin/%6cogin"
            assertThat(filter.call(uri = encoded).passed()).isTrue()
            assertThat(filter.call(uri = encoded).passed()).isTrue()

            val blocked = filter.call(uri = encoded)

            assertThat(blocked.passed()).isFalse()
            assertThat(blocked.response.status).isEqualTo(429)
            assertThat(filter.call().passed()).isFalse()
        }

        @Test
        fun `정책에 없는 경로는 몇 번을 불러도 통과한다`() {
            val filter = filter()

            repeat(5) {
                assertThat(filter.call(uri = "/api/v1/meals").passed()).isTrue()
            }
        }

        @Test
        fun `비활성화하면 한도를 넘어도 통과한다`() {
            val filter = filter(enabled = false)

            repeat(5) {
                assertThat(filter.call().passed()).isTrue()
            }
        }

        @Test
        fun `IP가 다르면 따로 센다`() {
            val filter = filter()
            filter.call(ip = "1.1.1.1")
            filter.call(ip = "1.1.1.1")

            assertThat(filter.call(ip = "1.1.1.1").passed()).isFalse()
            assertThat(filter.call(ip = "2.2.2.2").passed()).isTrue()
        }

        @Test
        fun `프록시 헤더를 설정하면 헤더의 IP로 센다`() {
            val filter = filter(header = "CF-Connecting-IP")
            val first = mapOf("CF-Connecting-IP" to "7.7.7.7")
            filter.call(headers = first)
            filter.call(headers = first)

            assertThat(filter.call(headers = first).passed()).isFalse()
            assertThat(filter.call(headers = mapOf("CF-Connecting-IP" to "8.8.8.8")).passed()).isTrue()
        }

        @Test
        fun `USER 키는 같은 IP여도 사용자별로 센다`() {
            val filter = filter(policy(KeyType.USER))
            login("user-a")
            filter.call()
            filter.call()

            assertThat(filter.call().passed()).isFalse()

            login("user-b")
            assertThat(filter.call().passed()).isTrue()
        }

        @Test
        fun `USER 키라도 로그인하지 않았으면 IP로 센다`() {
            val filter = filter(policy(KeyType.USER))
            filter.call(ip = "1.1.1.1")
            filter.call(ip = "1.1.1.1")

            assertThat(filter.call(ip = "1.1.1.1").passed()).isFalse()
            assertThat(filter.call(ip = "2.2.2.2").passed()).isTrue()
        }
    }

    private fun login(name: String) {
        val authentication = UsernamePasswordAuthenticationToken(name, null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication
    }
}
