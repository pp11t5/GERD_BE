package com.gerd.domain.symptom.service

import com.gerd.domain.symptom.dto.SymptomPatternAnalysisDTO
import com.gerd.domain.symptom.dto.SymptomResponseDTO
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.exception.SymptomErrorCode
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

@Component
class SymptomConverter(
    private val objectMapper: ObjectMapper,
) {

    fun parseOccurredAt(raw: String?): LocalDateTime {
        if (raw == null) throw GeneralException(SymptomErrorCode.INVALID_DATE_TIME)
        return try {
            OffsetDateTime.parse(raw).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            throw GeneralException(SymptomErrorCode.INVALID_DATE_TIME)
        }
    }

    fun toResponse(symptom: Symptom, linkedMeal: SymptomResponseDTO.LinkedMealDTO): SymptomResponseDTO =
        SymptomResponseDTO(
            symptomId = symptom.externalId?.toString() ?: throw GeneralException(SymptomErrorCode.SYMPTOM_NOT_FOUND),
            symptomState = symptom.symptomState,
            stateTitle = symptom.symptomState.code,
            symptomTypes = symptom.symptomTypes.toList(),
            occurredAt = symptom.occurredAt.atOffset(ZoneOffset.ofHours(9)).toString(),
            linkedMeal = linkedMeal,
            analysis = deserializeAnalysis(symptom.analysisJson, symptom.user.nickname),
        )

    private fun deserializeAnalysis(json: String?, nickname: String?): SymptomResponseDTO.AnalysisDTO? {
        val analysis = json?.let {
            runCatching { objectMapper.readValue(it, SymptomPatternAnalysisDTO::class.java) }.getOrNull()
        } ?: return null
        return SymptomResponseDTO.AnalysisDTO(
            title = "${nickname ?: "사용자"} 님을 위한 맞춤 분석이에요",
            items = listOf(
                SymptomResponseDTO.Item(
                    emphasis = analysis.pattern,
                    body = analysis.advice,
                ),
            ),
        )
    }
}
