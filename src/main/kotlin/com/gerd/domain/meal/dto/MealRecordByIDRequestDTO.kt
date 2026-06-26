package com.gerd.domain.meal.dto

import com.gerd.global.validation.ValidOffsetDateTime
import io.swagger.v3.oas.annotations.media.Schema

data class MealRecordByIDRequestDTO(
    @field:ValidOffsetDateTime(message = "먹은 시각은 ISO-8601 offset 형식이어야 합니다.")
    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함). 미전달 시 서버 현재 시각", example = "2026-06-11T12:30:00+09:00", nullable = true)
    val eatenAt: String? = null,
)
