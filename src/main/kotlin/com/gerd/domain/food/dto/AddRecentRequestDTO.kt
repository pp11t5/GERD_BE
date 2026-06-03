package com.gerd.domain.food.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

// 최근 본 음식 추가 요청 — 음식 상세 진입 시 해당 음식의 externalId 전달
data class AddRecentRequestDTO(
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    @field:NotBlank(message = "음식 식별자는 필수입니다.")
    val foodExternalId: String,
)
