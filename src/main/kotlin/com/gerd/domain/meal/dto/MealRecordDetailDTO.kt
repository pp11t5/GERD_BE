package com.gerd.domain.meal.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 식사 기록 DTO
 */
data class MealRecordDetailDTO(
    @field:Schema(description = "식사 기록 외부 식별자(UUID)", example = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealId: String,

    @field:Schema(description = "끼니 묶음 키(UUID)", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    val mealGroupId: String,

    @field:Schema(description = "먹은 시각 ISO-8601(offset 포함)", example = "2026-06-11T12:30:00+09:00")
    val eatenAt: String,

    @field:Schema(description = "추가 메모 (미작성 시 null)", example = "점심 후 잠깐 누웠더니 답답했음", nullable = true)
    val memo: String?,

    @field:Schema(description = "연결된 상태 기록(없으면 null)")
    val stateRecords: StateRecordDTO?,
) {

    data class MealFoodDetailDTO(
        @field:Schema(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        val externalId: String,

        @field:Schema(description = "음식 이름", example = "된장찌개")
        val name: String,

        @field:Schema(description = "대표 음식 분류 code", example = "soup_stew", nullable = true)
        val category: String?,

        @field:Schema(description = "먹은 시각 ISO-8601(offset 포함)", example = "2026-06-11T12:30:00+09:00")
        val eatenAt: String,

    )
}
