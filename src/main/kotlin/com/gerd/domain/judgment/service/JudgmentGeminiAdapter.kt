package com.gerd.domain.judgment.service

import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.global.ai.gemini.GeminiClient
import com.gerd.global.ai.gemini.GeminiRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

@Component
class JudgmentGeminiAdapter(
    private val geminiClient: GeminiClient,
    private val objectMapper: ObjectMapper,
) {

    fun generateJudgment(
        systemInstruction: String,
        userContent: String,
        responseSchema: Map<String, Any>,
    ): LlmJudgmentDTO? {
        val text = geminiClient.generateJson(
            GeminiRequest(
                systemInstruction = systemInstruction,
                userContent = userContent,
                responseSchema = responseSchema,
            ),
        ) ?: return null

        return try {
            val judgment = objectMapper.readValue(text, LlmJudgmentDTO::class.java)
            if (judgment.grade != JudgmentGrade.UNKNOWN && judgment.items.size != REQUIRED_ITEM_COUNT) {
                log.warn { "Gemini 판정 items 슬롯 수 불일치: ${judgment.items.size}" }
                return null
            }
            judgment
        } catch (e: Exception) {
            log.warn { "Gemini 판정 응답 파싱 실패: ${e.javaClass.simpleName} - ${e.message}" }
            null
        }
    }

    companion object {
        private const val REQUIRED_ITEM_COUNT = 2
    }
}
