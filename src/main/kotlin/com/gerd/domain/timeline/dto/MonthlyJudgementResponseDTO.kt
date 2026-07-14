package com.gerd.domain.timeline.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema

data class MonthlyJudgementResponseDTO(
    @field:Schema(description = "일(day of month)", example = "12")
    val day: Int,

    @field:Schema(description = "요일", example = "SAT")
    val dayOfWeek: String,

    @field:Schema(description = "판정 등급 목록, 최대 3개 (RECOMMEND·CAUTION·RISK·UNKNOWN)", example = "[\"RECOMMEND\", \"CAUTION\"]")
    val judgementList: List<JudgmentGrade>
)
