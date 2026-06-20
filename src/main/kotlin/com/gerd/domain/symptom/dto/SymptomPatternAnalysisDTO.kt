package com.gerd.domain.symptom.dto

/**
 * LLM 응답으로 받을 DTO
 */
data class SymptomPatternAnalysisDTO(
    val label: String,
    val pattern: String,
    val advice: String,
)
