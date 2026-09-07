package com.gerd.global.ai.gemini

import com.gerd.global.ai.TokenUsage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GeminiPricingTest {

    @Nested
    inner class 등록된_모델 {

        @Test
        fun `input, output 토큰을 각각 다른 단가로 환산한다`() {
            val usage = TokenUsage(promptTokens = 1_000_000, completionTokens = 1_000_000, totalTokens = 2_000_000)

            val cost = GeminiPricing.costUsd("gemini-3.5-flash-lite", usage)

            assertThat(cost).isEqualTo(0.30 + 2.50)
        }
    }

    @Nested
    inner class 미등록_모델 {

        @Test
        fun `단가표에 없으면 null을 반환한다`() {
            val usage = TokenUsage(promptTokens = 100, completionTokens = 50, totalTokens = 150)

            assertThat(GeminiPricing.costUsd("unknown-model", usage)).isNull()
        }
    }
}
