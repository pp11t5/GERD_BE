package com.gerd.domain.food.dto

import io.swagger.v3.oas.annotations.media.Schema

data class FoodCategoryDTO(
    @field:Schema(description = "분류 code (아이콘 키)", example = "soup_stew")
    val code: String,

    @field:Schema(description = "분류 표시명", example = "국·찌개")
    val displayName: String,
)
