package com.gerd.domain.mypage.dto

import com.gerd.domain.onboarding.entity.enums.DiseaseType
import io.swagger.v3.oas.annotations.media.Schema

data class MyPageSummaryResponseDTO(
    @field:Schema(description = "프로필 요약")
    val profile: ProfileSummary,
    @field:Schema(description = "음식 히스토리(도감 기준)")
    val foodHistory: FoodHistory,
    @field:Schema(description = "지난주 기록 요약")
    val weeklySummary: WeeklySummary,
) {
    data class ProfileSummary(
        @field:Schema(description = "닉네임", example = "서유진")
        val nickName: String,
        @field:Schema(description = "프로필 이미지", nullable = true)
        val profileImage: String? = null,
        @field:Schema(description = "질환명", example = "gerd")
        val disease: DiseaseType,
    )

    data class FoodHistory(
        @field:Schema(description = "도감 SAFE 음식 수", example = "43")
        val safeCount: Int,
        @field:Schema(description = "주의 음식 수", example = "8")
        val cautionCount: Int,
    )

    data class WeeklySummary(
        @field:Schema(description = "지난주 식사 기록 횟수(끼니 단위)", example = "14")
        val mealRecordCount: Int,
        @field:Schema(description = "지난주 증상 기록 횟수", example = "5")
        val recentSymptomCount: Int,
        @field:Schema(description = "현재 연속 기록 스트릭 일수", example = "7")
        val streakCount: Int,
        val mealCount: MealCount,
    )
}
