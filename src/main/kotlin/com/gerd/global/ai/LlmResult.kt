package com.gerd.global.ai

// 후보 텍스트 없이 usageMetadata만 오는 응답도 있어 text는 nullable — 그래도 비용은 집계해야 한다
data class LlmResult(
    val text: String?,
    val usage: TokenUsage? = null,
)

// 공급사마다 필드명이 달라 여기서 공통 형태로 정규화한다 (#81 LLM 비용 모니터링용 지표 수집)
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)
