package com.gerd.global.ai

data class LlmResult(
    val text: String,
    val usage: TokenUsage? = null,
)

// 공급사마다 필드명이 달라 여기서 공통 형태로 정규화한다 (#81 LLM 비용 모니터링용 지표 수집)
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)
