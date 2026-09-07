package com.gerd.global.ai

interface LlmClient {
    fun generateJson(request: LlmRequest): LlmResult?
}
