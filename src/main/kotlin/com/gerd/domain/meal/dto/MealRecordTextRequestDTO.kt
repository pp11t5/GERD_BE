package com.gerd.domain.meal.dto

import com.gerd.global.validation.ValidOffsetDateTime
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MealRecordTextRequestDTO(
    @field:NotBlank(message = "음식 이름은 필수입니다.")
    @field:Size(max = 100, message = "음식 이름은 100자 이하여야 합니다.")
    @field:Schema(description = "사용자가 입력한 음식 이름(텍스트)", example = "김치찌개 밀키트")
    val name: String,

    @field:ValidOffsetDateTime(message = "먹은 시각은 ISO-8601 offset 형식이어야 합니다.")
    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함) 미전달 시 서버 현재 시각", example = "2026-06-11T12:30:00+09:00", nullable = true)
    val eatenAt: String? = null,
)
