package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.global.ai.LlmClient
import com.gerd.global.ai.LlmRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

@Component
class JudgmentGeminiAdapter(
    private val llmClient: LlmClient,
    private val objectMapper: ObjectMapper,
) {

    @CircuitBreaker(name = "gemini-judgment", fallbackMethod = "fallback")
    fun generateJudgment(
        systemInstruction: String,
        userContent: String,
        responseSchema: Map<String, Any>,
    ): LlmJudgmentDTO? {
        val text = llmClient.generateJson(
            LlmRequest(
                systemInstruction = systemInstruction,
                userContent = userContent,
                responseSchema = responseSchema,
            ),
        ) ?: return null

        return try {
            val judgment = objectMapper.readValue(text, LlmJudgmentDTO::class.java)
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

    // CB OPEN 또는 예외 발생 시 null 반환 — 서비스 레이어가 CAUTION 폴백으로 처리
    private fun fallback(
        systemInstruction: String,
        userContent: String,
        responseSchema: Map<String, Any>,
        e: Exception,
    ): LlmJudgmentDTO? {
        log.warn { "판정 CB 폴백: ${e.javaClass.simpleName}" }
        return null
    }

    companion object {
        private const val REQUIRED_ITEM_COUNT = 2
    }
}
