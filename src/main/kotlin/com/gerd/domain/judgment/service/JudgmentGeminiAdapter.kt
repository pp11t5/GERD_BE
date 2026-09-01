package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.global.ai.LlmClient
import com.gerd.global.ai.LlmRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

@Component
class JudgmentGeminiAdapter(
    private val llmClient: LlmClient,
    private val objectMapper: ObjectMapper,
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

        // 비용 모니터링용 지표 — 집계·알림은 #81에서 별도로 다룬다
        result.usage?.let {
            log.info { "Gemini 판정 토큰 사용량: prompt=${it.promptTokens} completion=${it.completionTokens} total=${it.totalTokens}" }
        }

        return try {
            val judgment = objectMapper.readValue(result.text, LlmJudgmentDTO::class.java)
            if (judgment.items.size != REQUIRED_ITEM_COUNT) {
                log.warn { "Gemini 판정 items 슬롯 수 불일치: ${judgment.items.size}" }
                return null
            }
            judgment
        } catch (e: Exception) {
            log.warn { "Gemini 판정 응답 파싱 실패: ${e.javaClass.simpleName} - ${e.message}" }
            null
        }
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
