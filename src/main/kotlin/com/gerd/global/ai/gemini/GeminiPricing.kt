package com.gerd.global.ai.gemini

import com.gerd.global.ai.TokenUsage

// Paid Tier, non-batch 기준 — https://ai.google.dev/gemini-api/docs/pricing (2026-08 확인)
object GeminiPricing {

    private data class Rate(
        val inputUsdPerMillionTokens: Double,
        val outputUsdPerMillionTokens: Double,
    )

    private val RATES = mapOf(
        "gemini-3.5-flash-lite" to Rate(inputUsdPerMillionTokens = 0.30, outputUsdPerMillionTokens = 2.50),
    )

    fun costUsd(model: String, usage: TokenUsage): Double? {
        val rate = RATES[model] ?: return null
        return usage.promptTokens * rate.inputUsdPerMillionTokens / MILLION +
            usage.completionTokens * rate.outputUsdPerMillionTokens / MILLION
    }

    private const val MILLION = 1_000_000.0
}
