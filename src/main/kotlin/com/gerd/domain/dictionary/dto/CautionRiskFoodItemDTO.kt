package com.gerd.domain.dictionary.dto

import com.gerd.domain.dictionary.entity.enums.DictionaryType
import io.swagger.v3.oas.annotations.media.Schema

// 주의·위험 음식 목록 항목 — CAUTION/RISK를 구분해야 하므로 type을 포함
data class CautionRiskFoodItemDTO(
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val foodId: String,

    @field:Schema(description = "음식 이름", example = "양파")
    val name: String,

    @field:Schema(description = "대표 음식 분류 code", example = "vegetable", nullable = true)
    val code: String?,

    @field:Schema(description = "도감 상태(caution | risk)", example = "caution")
    val type: DictionaryType,
)
