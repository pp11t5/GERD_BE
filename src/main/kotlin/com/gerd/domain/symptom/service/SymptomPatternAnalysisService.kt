package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.dto.SymptomPatternAnalysisDTO
import com.gerd.domain.symptom.dto.SymptomPatternAnalysisInputDTO
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.global.ai.gemini.GeminiClient
import com.gerd.global.ai.gemini.GeminiRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

@Service
class SymptomPatternAnalysisService(
    private val symptomPatternAnalysisInputReader: SymptomPatternAnalysisInputReader,
    private val symptomPatternAnalysisPromptBuilder: SymptomPatternAnalysisPromptBuilder,
    private val geminiClient: GeminiClient,
    private val objectMapper: ObjectMapper,
) {

    // 증상 상세에서 맞춤 패턴 분석 생성
    fun generate(symptom: Symptom, userId: Long): SymptomPatternAnalysisResult {
        val input = symptomPatternAnalysisInputReader.read(symptom, userId)
        val analysis = if (!input.features.hasReliablePattern) {
            // 데이터가 없다면 기록 부족 패턴 분석을 반환
            buildInsufficientDataFallback()
        } else {
            generateByGemini(input)
                ?: return symptom.analysisJson
                    ?.let { SymptomPatternAnalysisResult(analysisJson = it, shouldUpdate = false) }
                    ?: SymptomPatternAnalysisResult(
                        analysisJson = objectMapper.writeValueAsString(buildGeminiFailureFallback()),
                        shouldUpdate = false,
                    )
        }
        return SymptomPatternAnalysisResult(
            analysisJson = objectMapper.writeValueAsString(analysis),
            shouldUpdate = true,
        )
    }

    // Gemini 모델을 이용한 증상 패턴 분석 생성
    private fun generateByGemini(input: SymptomPatternAnalysisInputDTO): SymptomPatternAnalysisDTO? {
        val json = geminiClient.generateJson(
            GeminiRequest(
                systemInstruction = symptomPatternAnalysisPromptBuilder.buildSystemInstruction(),
                userContent = symptomPatternAnalysisPromptBuilder.buildUserContent(input),
                responseSchema = symptomPatternAnalysisPromptBuilder.buildResponseSchema(),
            ),
        ) ?: return null

        return try {
            objectMapper.readValue(json, SymptomPatternAnalysisDTO::class.java)
        } catch (e: Exception) {
            log.warn { "Gemini 증상 패턴 분석 응답 파싱 실패: ${e.javaClass.simpleName} - ${e.message}" }
            null
        }
    }

    // 기록 부족 시 패턴 분석
    private fun buildInsufficientDataFallback(): SymptomPatternAnalysisDTO =
        SymptomPatternAnalysisDTO(
            label = "기록 부족",
            pattern = "아직 패턴을 말하기엔 연결된 기록이 적어요.",
            advice = "식사와 증상을 몇 번 더 연결해 기록하면 더 정확히 볼 수 있어요.",
        )

    // 기본
    private fun buildGeminiFailureFallback(): SymptomPatternAnalysisDTO =
        SymptomPatternAnalysisDTO(
            label = "기록 부족",
            pattern = "지금은 패턴 분석을 만들기 어려워요.",
            advice = "잠시 후 다시 확인하거나 식사와 증상을 조금 더 기록해 주세요.",
        )
}

// 분석 결과, 업데이트 필요 여부 결과 DTO
data class SymptomPatternAnalysisResult(
    val analysisJson: String,
    val shouldUpdate: Boolean,
)
