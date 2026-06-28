package com.gerd.domain.streak.dto

import io.swagger.v3.oas.annotations.media.Schema

data class UserStreakResponseDTO(
    @field:Schema(description = "연속 편안한 일수", example = "5")
    val streak: Int,
)
