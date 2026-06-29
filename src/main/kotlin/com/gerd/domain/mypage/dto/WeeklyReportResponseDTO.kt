package com.gerd.domain.mypage.dto

import io.swagger.v3.oas.annotations.media.Schema

data class WeeklyReportResponseDTO(
    @field:Schema(description = "주간 시작일 (ISO-8601)", example = "2026-05-10")
    val startDate: String,
    @field:Schema(description = "주간 종료일 (ISO-8601)", example = "2026-05-16")
    val endDate: String,
    @field:Schema(description = "주차 레이블", example = "2026년 5월 둘째주")
    val weekLabel: String,
    @field:Schema(description = "속 편한 음식 현황")
    val comfortableState: ComfortableState,
    @field:Schema(description = "식단 분포")
    val mealCount: MealCount,
) {
    data class ComfortableState(
        @field:Schema(description = "현재 연속 기록 스트릭 일수", example = "3")
        val streakCount: Int,
        @field:Schema(description = "지난주 권장(RECOMMEND) 끼니 수", example = "6")
        val recommendedMealCount: Int,
        @field:Schema(description = "전체 끼니 중 편안한 음식 비율(%)", example = "84")
        val percentage: Double,
    )
}