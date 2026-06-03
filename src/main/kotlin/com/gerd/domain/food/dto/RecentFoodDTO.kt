package com.gerd.domain.food.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// 최근 본 음식 1건 — recentId는 단건 삭제용 식별자(food_search_history PK)
data class RecentFoodDTO(
    @field:Schema(description = "최근 검색 항목 ID(단건 삭제용)", example = "1024")
    val recentId: Long,

    @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val foodExternalId: String,

    @field:Schema(description = "음식 이름", example = "된장찌개")
    val name: String,

    @field:Schema(description = "음식 분류 목록")
    val categories: List<FoodCategoryDTO>,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @field:Schema(description = "마지막으로 본 시각", example = "2026-06-03 08:12:00")
    val searchedAt: LocalDateTime,
)
