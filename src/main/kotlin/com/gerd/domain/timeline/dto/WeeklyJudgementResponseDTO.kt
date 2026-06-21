package com.gerd.domain.timeline.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class WeeklyJudgementResponseDTO(
    @field:Schema(description = "날짜(yyyy-MM-dd)", example = "2024-06-01")
    val date : LocalDate,

    @field:Schema(description = "요일", example = "SAT")
    val dayOfWeek: String,

    @field:Schema(description = "판정 등급 목록, 최대 3개", example = "[\"RECOMMEND\", \"CAUTION\"]")
    val judgementList: List<JudgmentGrade>

)
