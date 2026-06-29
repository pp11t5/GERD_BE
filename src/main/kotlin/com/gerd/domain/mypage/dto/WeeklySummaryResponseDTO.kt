package com.gerd.domain.mypage.dto

import io.swagger.v3.oas.annotations.media.Schema

data class WeeklySummaryResponseDTO(
    @field:Schema(description = "지난주 식사 기록 횟수(끼니 단위)", example = "14")
    val mealRecordCount: Int,
    @field:Schema(description = "지난주 증상 기록 횟수", example = "5")
    val recentSymptomCount: Int,
    @field:Schema(description = "현재 연속 기록 스트릭 일수", example = "7")
    val streakCount: Int,
    val mealCount: MealCount,
)
