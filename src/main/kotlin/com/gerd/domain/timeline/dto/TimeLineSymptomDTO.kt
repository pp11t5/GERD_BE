package com.gerd.domain.timeline.dto

import com.gerd.domain.symptom.entity.enums.SymptomState
import io.swagger.v3.oas.annotations.media.Schema

data class TimeLineSymptomDTO(
    @field:Schema(description = "식사 후 경과 시간 (분)", example = "30")
    val afterMealMinutes: Int,
    @field:Schema(description = "증상 상태", example = "MILD")
    val symptomState: SymptomState,
    @field:Schema(description = "대표 증상 유형, 최대 2개", example = "[\"속쓰림\", \"목 이물감\"]")
    val symptomTypes: List<String>,
    @field:Schema(description = "대표 증상 유형 외 기타 증상 개수", example = "1")
    val etcCount: Int,
)
