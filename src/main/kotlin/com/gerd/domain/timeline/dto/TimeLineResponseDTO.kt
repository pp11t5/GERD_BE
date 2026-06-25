package com.gerd.domain.timeline.dto

import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.domain.timeline.enums.TimeLineType
import io.swagger.v3.oas.annotations.media.Schema

data class TimeLineResponseDTO(
    val items: List<TimeLineItemDTO>
)

sealed class TimeLineItemDTO {

    data class Single(
        @field:Schema(description = "타임라인 타입", example = "single")
        val timeLineType: TimeLineType,
        @field:Schema(description = "식사 기록 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        val mealRecordId: String,
        @field:Schema(description = "식사 시각 (ISO 8601)", example = "2026-06-21T12:30:00")
        val mealRecordDateTime: String,
        @field:Schema(description = "대표 음식 이름", example = "김치찌개")
        val mealFoodName: String,
        @field:Schema(description = "판정 등급", example = "NORMAL")
        val grade: JudgmentGrade,
        @field:Schema(description = "기타 음식 개수", example = "3")
        val etcCount: Int,
    ) : TimeLineItemDTO()

    data class Group(
        @field:Schema(description = "타임라인 타입", example = "group")
        val timeLineType: TimeLineType,
        @field:Schema(description = "식사 기록 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        val mealRecordId: String,
        @field:Schema(description = "식사 시각 (ISO 8601)", example = "2026-06-21T12:30:00")
        val mealRecordDateTime: String,
        @field:Schema(description = "대표 음식, 최대 2개", example = "[\"김치찌개\", \"된장찌개\"]")
        val representativeFoods: List<String>,
        @field:Schema(description = "기타 음식 개수", example = "3")
        val etcCount: Int,
        val connectedSymptoms: ConnectedSymptom?,
    ) : TimeLineItemDTO()

    data class Symptom(
        @field:Schema(description = "타임라인 타입", example = "symptom")
        val timeLineType: TimeLineType,
        @field:Schema(description = "증상 상태", example = "MILD")
        val symptomState: SymptomState,
        @field:Schema(description = "식사 후 경과 시간 (분)", example = "30")
        val afterMealMinutes: Int,
        @field:Schema(description = "증상 발생 시각 (ISO 8601)", example = "2026-06-21T13:00:00")
        val occurredAt: String,
    ) : TimeLineItemDTO()

    data class ConnectedSymptom(
        @field:Schema(description = "증상 상태") // 맞게 수정 필요
        val symptomState: SymptomState,
        @field:Schema(description = "식사 후 경과 시간 (분)", example = "30")
        val afterMealMinutes: Int,
        @field:Schema(description = "대표 증상,최대 2개", example = "[\"throat_foreign_body\", \"stomachache\"]") // 실제에 맞출 필요
        val representativeSymptoms: List<SymptomType>,
        @field:Schema(description = "기타 증상 개수, example")
        val etcCount: Int,

    )

}
