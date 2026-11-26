package com.gerd.global.ratelimit

import com.gerd.global.config.properties.RateLimitProperties.Policy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

class RateLimitBucketsTest {
    private val buckets = RateLimitBuckets()

    private fun policy(
        name: String = "login",
        limit: Int = 2,
    ) = Policy(
        name = name,
        limitForPeriod = limit,
        refreshPeriod = Duration.ofMinutes(10),
    )

    @Nested
    inner class `tryConsume` {
        @Test
        fun `한도 안의 요청은 허용한다`() {
            val policy = policy(limit = 2)

            assertThat(buckets.tryConsume(policy, "ip:1").allowed).isTrue()
            assertThat(buckets.tryConsume(policy, "ip:1").allowed).isTrue()
        }

        @Test
        fun `한도를 넘으면 거절하고 재시도 대기 시간을 알려 준다`() {
            val policy = policy(limit = 2)
            buckets.tryConsume(policy, "ip:1")
            buckets.tryConsume(policy, "ip:1")

            val result = buckets.tryConsume(policy, "ip:1")

            assertThat(result.allowed).isFalse()
            assertThat(result.retryAfterSeconds).isGreaterThan(0)
        }

        @Test
        fun `키가 다르면 버킷도 따로 센다`() {
            val policy = policy(limit = 1)
            buckets.tryConsume(policy, "ip:1")

            assertThat(buckets.tryConsume(policy, "ip:1").allowed).isFalse()
            assertThat(buckets.tryConsume(policy, "ip:2").allowed).isTrue()
        }

        @Test
        fun `정책이 다르면 같은 키도 따로 센다`() {
            val admin = policy(name = "admin-login", limit = 1)
            val token = policy(name = "auth-token", limit = 1)
            buckets.tryConsume(admin, "ip:1")

            assertThat(buckets.tryConsume(admin, "ip:1").allowed).isFalse()
            assertThat(buckets.tryConsume(token, "ip:1").allowed).isTrue()
        }
    }
}
