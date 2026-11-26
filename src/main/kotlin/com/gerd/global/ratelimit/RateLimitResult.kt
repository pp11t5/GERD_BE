package com.gerd.global.ratelimit

data class RateLimitResult(
    val allowed: Boolean,
    val retryAfterSeconds: Long = 0,
)
