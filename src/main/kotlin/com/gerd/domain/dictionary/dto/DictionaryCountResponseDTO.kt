package com.gerd.domain.dictionary.dto

import io.swagger.v3.oas.annotations.media.Schema

data class DictionaryCountResponseDTO(
    @field:Schema(description = "안전 음식 수")
    val safeCount: Int,

    @field:Schema(description = "주의·위험 음식 수")
    val cautionRiskCount: Int,
)
