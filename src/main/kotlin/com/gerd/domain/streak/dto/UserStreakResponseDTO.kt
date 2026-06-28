package com.gerd.domain.streak.dto

import io.swagger.v3.oas.annotations.media.Schema

data class UserStreakResponseDTO(
    @field:Schema(description = "연속 기록일", example = "5")
    val streak: Int,
)
