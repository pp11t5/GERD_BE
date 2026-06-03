package com.gerd.domain.food.dto

import io.swagger.v3.oas.annotations.media.Schema

// 음식 분류 (응답엔 image_url 대신 카테고리를 내려 클라이언트가 카테고리별 대표 이미지를 매칭한다 — D6)
data class FoodCategoryDTO(
    @field:Schema(description = "분류 code", example = "soup_stew")
    val code: String,

    @field:Schema(description = "분류 표시명", example = "국·찌개·탕")
    val displayName: String,
)
