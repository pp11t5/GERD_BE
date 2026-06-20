package com.gerd.global.ai.gemini

data class GeminiRequest(
    val systemInstruction: String,
    val userContent: String,
    val responseSchema: Map<String, Any>,
)
