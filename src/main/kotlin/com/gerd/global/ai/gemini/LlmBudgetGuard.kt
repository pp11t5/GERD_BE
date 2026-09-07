package com.gerd.global.ai.gemini

import com.gerd.global.config.properties.LlmBudgetProperties
import com.gerd.infra.monitoring.sentry.DiscordEmbed
import com.gerd.infra.monitoring.sentry.DiscordEmbedField
import com.gerd.infra.monitoring.sentry.DiscordWebhookClient
import com.gerd.infra.monitoring.sentry.DiscordWebhookMessage
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

// 인메모리·단일 인스턴스 전제 — 여러 인스턴스로 스케일아웃되면 인스턴스별로 따로 누적돼 알림이 실제보다 늦게 뜰 수 있음
// 판정 1건당 1번만 호출되는 낮은 빈도라 synchronized로 충분 — 누적·alerted 판정을 한 트랜잭션으로 묶어 중복 알림을 막는다
@Component
class LlmBudgetGuard(
    private val llmBudgetProperties: LlmBudgetProperties,
    private val discordWebhookClient: DiscordWebhookClient,
) {

    private var state = DailyState(today(), 0.0, alerted = false)

    fun record(feature: String, costUsd: Double) {
        if (llmBudgetProperties.dailyLimitUsd <= 0.0) return

        val spentUsdToAlert = synchronized(this) {
            if (state.date != today()) {
                state = DailyState(today(), 0.0, alerted = false)
            }
            state = state.copy(spentUsd = state.spentUsd + costUsd)

            if (state.spentUsd >= llmBudgetProperties.dailyLimitUsd && !state.alerted) {
                state = state.copy(alerted = true)
                state.spentUsd
            } else {
                null
            }
        }

        spentUsdToAlert?.let { alert(feature, it) }
    }

    private fun alert(feature: String, spentUsd: Double) {
        discordWebhookClient.send(
            llmBudgetProperties.discordWebhookUrl,
            DiscordWebhookMessage(
                embeds = listOf(
                    DiscordEmbed(
                        title = "💸 LLM 일일 예산 초과",
                        description = "feature=$feature 오늘 누적 비용이 예산을 초과했어요",
                        url = null,
                        color = BUDGET_ALERT_COLOR,
                        fields = listOf(
                            DiscordEmbedField(name = "Spent", value = "$%.4f".format(spentUsd), inline = true),
                            DiscordEmbedField(
                                name = "Limit",
                                value = "$%.4f".format(llmBudgetProperties.dailyLimitUsd),
                                inline = true,
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))

    private data class DailyState(val date: LocalDate, val spentUsd: Double, val alerted: Boolean)

    companion object {
        private const val BUDGET_ALERT_COLOR = 15_158_332
    }
}
