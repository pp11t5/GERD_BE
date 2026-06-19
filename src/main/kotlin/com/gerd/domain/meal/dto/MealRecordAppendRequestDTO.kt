package com.gerd.domain.meal.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class MealRecordAppendRequestDTO(
    @field:NotBlank
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val foodExternalId: String,

    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함). 미전달 시 서버 현재 시각", example = "2026-06-11T12:30:00+09:00", nullable = true)
    val eatenAt: String? = null,

    @field:Schema(description = "끼니 식별자(UUID). 전달 시 같은 끼니에 추가", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f", nullable = true)
    val mealRecordId: String? = null,
)
