package com.gerd.global.ai

import org.springframework.web.client.ResourceAccessException

class LlmTimeoutException(cause: ResourceAccessException) :
    RuntimeException("LLM 응답 대기 시간 초과", cause)
