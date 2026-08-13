package com.gerd.domain.mypage.dto

import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetTime

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
    @field:Schema(description = "증상 통계")
    val recordedSymptom: RecordedSymptom,
    @field:Schema(description = "면책 고지")
    val disclaimer: String = JudgmentResponseDTO.DEFAULT_DISCLAIMER,
    @field:Schema(description = "판정에 사용된 근거 자료")
    val references: List<JudgmentResponseDTO.ReferenceDTO> = JudgmentResponseDTO.DEFAULT_REFERENCES,
) {
    data class ComfortableState(
        @field:Schema(description = "현재 연속 기록 스트릭 일수", example = "3")
        val streakCount: Int,
        @field:Schema(description = "지난주 권장(RECOMMEND) 끼니 수", example = "6")
        val recommendedMealCount: Int,
        @field:Schema(description = "전체 끼니 중 편안한 음식 비율(%)", example = "84")
        val percentage: Double,
    )

    data class RecordedSymptom(
        @field:Schema(description = "증상 기록 횟수", example = "5")
        val symptomCount: Int,
        @field:Schema(description = "평균 증상 기록 시간 (HH:mm:ssZ)", example = "08:00:00+09:00")
        val averageTime: OffsetTime,
        @field:Schema(description = "평균 강도", example = "3")
        val averageLevel: Int,
        @field:Schema(description = "목 이물감 횟수", example = "1")
        val throatForeignBodyCount : Int,
        @field:Schema(description = "신물 횟수", example = "1")
        val acidRefluxCount : Int,
        @field:Schema(description = "기침 횟수", example = "1")
        val coughCount : Int,
        @field:Schema(description = "가슴 답답함 횟수", example = "1")
        val chestTightnessCount : Int,
    )
}