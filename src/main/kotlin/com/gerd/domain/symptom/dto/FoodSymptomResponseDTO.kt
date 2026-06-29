package com.gerd.domain.symptom.dto

import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import io.swagger.v3.oas.annotations.media.Schema

data class FoodSymptomResponseDTO(
    @field:Schema(description = "증상 기록 외부 식별자(UUID) — 상세 진입용", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val symptomId: String,

    @field:Schema(description = "증상 상태: comfortable(편안), good(양호), normal(보통), uncomfortable(불편), severe(심각)", example = "uncomfortable")
    val symptomState: SymptomState,

    @field:Schema(description = "느낀 증상 목록", example = "[\"acid_reflux\", \"chest_tightness\"]")
    val symptomTypes: List<SymptomType>,

    @field:Schema(description = "증상 발생 시각 (ISO 8601)", example = "2026-06-21T13:00:00")
    val occurredAt: String,

    @field:Schema(description = "연결된 끼니 식별자(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealRecordId: String,

    @field:Schema(description = "식사 후 경과 시간 (분)", example = "70")
    val afterMealMinutes: Int,
)
