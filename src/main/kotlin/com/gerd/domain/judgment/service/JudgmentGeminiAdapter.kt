package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.global.ai.LlmClient
import com.gerd.global.ai.LlmRequest
import com.gerd.global.ai.TokenUsage
import com.gerd.global.ai.gemini.GeminiPricing
import com.gerd.global.ai.gemini.LlmBudgetGuard
import com.gerd.global.config.properties.GeminiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

@Component
class JudgmentGeminiAdapter(
    private val llmClient: LlmClient,
    private val objectMapper: ObjectMapper,
    private val geminiProperties: GeminiProperties,
    private val llmBudgetGuard: LlmBudgetGuard,
) {

    @Retry(name = "gemini-judgment", fallbackMethod = "fallback")
    @CircuitBreaker(name = "gemini-judgment")
    fun generateJudgment(
        systemInstruction: String,
        userContent: String,
        responseSchema: Map<String, Any>,
    ): LlmJudgmentDTO? {
        val result = llmClient.generateJson(
            LlmRequest(
                systemInstruction = systemInstruction,
                userContent = userContent,
                responseSchema = responseSchema,
            ),
        ) ?: return null

        val judgment = try {
            val parsed = objectMapper.readValue(result.text, LlmJudgmentDTO::class.java)
            if (parsed.items.size != REQUIRED_ITEM_COUNT) {
                log.warn { "Gemini 판정 items 슬롯 수 불일치: ${parsed.items.size}" }
                null
            } else {
                parsed
            }
        } catch (e: Exception) {
            log.warn { "Gemini 판정 응답 파싱 실패: ${e.javaClass.simpleName} - ${e.message}" }
            null
        }

        result.usage?.let { logTokenUsage(it, judgment?.grade) }

        return judgment
    }

    // 비용 모니터링용 지표(#81) — 집계는 로그 수집기, 예산 알림은 llmBudgetGuard가 담당
    private fun logTokenUsage(usage: TokenUsage, grade: JudgmentGrade?) {
        val costUsd = GeminiPricing.costUsd(geminiProperties.model, usage)
        log.info(
            "feature={} model={} promptTokens={} completionTokens={} totalTokens={} costUsd={} grade={}",
            kv("feature", "judgment"),
            kv("model", geminiProperties.model),
            kv("promptTokens", usage.promptTokens),
            kv("completionTokens", usage.completionTokens),
            kv("totalTokens", usage.totalTokens),
            kv("costUsd", costUsd),
            kv("grade", grade),
        )
        costUsd?.let { llmBudgetGuard.record("judgment", it) }
    }

    // 재시도 소진 또는 CB OPEN 시 null 반환 — 서비스 레이어가 CAUTION 폴백으로 처리
    private fun fallback(
        systemInstruction: String,
        userContent: String,
        responseSchema: Map<String, Any>,
        e: Exception,
    ): LlmJudgmentDTO? {
        log.error(e) { "판정 CB 폴백: ${e.javaClass.simpleName}" }
        return null
    }

    companion object {
        private const val REQUIRED_ITEM_COUNT = 2
    }
}
