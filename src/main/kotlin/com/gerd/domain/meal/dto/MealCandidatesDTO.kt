package com.gerd.domain.meal.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 원인 식사 후보 DTO — 날짜별 그룹. 끼니당 대표 음식 1개 + 나머지 개수만 노출
 */
data class MealCandidatesDTO(
    @field:Schema(description = "날짜(YYYY-MM-DD)", example = "2026-05-12")
    val date: String,

    @field:Schema(description = "해당 날짜의 끼니 목록 (대표 시각 오름차순)")
    val meals: List<MealCandidateItem>,
) {
    data class MealCandidateItem(
        @field:Schema(description = "끼니 식별자(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        val mealRecordId: String,

        @field:Schema(description = "대표 음식 (끼니 첫 음식)")
        val representativeFood: RepresentativeFood,

        @field:Schema(description = "대표 음식 외 나머지 음식 개수 — 0이면 대표 음식만 표시", example = "4")
        val otherFoodCount: Int,

        @field:Schema(description = "끼니 대표 시각 ISO-8601(offset 포함)", example = "2026-05-12T13:36:00+09:00")
        val eatenAt: String,
    )

    data class RepresentativeFood(
        @field:Schema(description = "음식 이름", example = "된장찌개")
        val name: String,

        @field:Schema(description = "대표 음식 분류 code (아이콘 표시용)", example = "soup_stew", nullable = true)
        val category: String?,
    )
}
