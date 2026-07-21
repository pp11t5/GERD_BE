package com.gerd.domain.food.dto

import io.swagger.v3.oas.annotations.media.Schema

data class AdminUserFoodDTO(
    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val externalId: String,

    @field:Schema(description = "음식 이름", example = "된장찌개")
    val name: String,
)
