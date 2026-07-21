package com.gerd.domain.food.dto

import io.swagger.v3.oas.annotations.media.Schema

data class FoodSearchResultDTO(
    @field:Schema(description = "검색 결과 목록(없으면 빈 배열)")
    val foods: List<FoodSummaryDTO>,

    @field:Schema(description = "검색어와 완전 일치하는 음식이 있으면 true. false일 때 직접 입력 판정(GET /foods/judgment?name=...) 유도")
    val hasExactMatch: Boolean,
)
