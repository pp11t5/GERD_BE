package com.gerd.domain.meal.dto

import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 증상 기록 DTO
 */
data class StateRecordDTO(
    @field:Schema(description = "증상 기록 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val stateRecordId: String,

    @field:Schema(description = "상태", example = "편안해요")
    val label: SymptomState,

    @field:Schema(description = "기록 날짜", example = "2026-05-08")
    val date: String,

    @field:Schema(description = "기록 시점, 식후 n분", example = "90")
    val timingMinutes: Int,
)
