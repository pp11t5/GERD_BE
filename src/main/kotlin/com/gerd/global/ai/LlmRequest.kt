package com.gerd.global.ai

data class LlmRequest(
    val systemInstruction: String,
    val userContent: String,
    val responseSchema: Map<String, Any>,
)
