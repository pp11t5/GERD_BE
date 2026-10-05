package com.gerd.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "llm.budget")
data class LlmBudgetProperties(
    // 0이면 알림 비활성 — 실제 예산은 운영 판단 필요, 확정 전 플레이스홀더
    var dailyLimitUsd: Double = 0.0,
    var discordWebhookUrl: String = "",
)
