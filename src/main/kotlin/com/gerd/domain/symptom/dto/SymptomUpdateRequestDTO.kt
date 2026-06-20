package com.gerd.domain.symptom.dto

import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.global.validation.ValidOffsetDateTime
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.UUID

/**
 * 증상 기록 전체 수정 요청
 */
data class SymptomUpdateRequestDTO(
    @field:Schema(description = "지금 속 상태 - 단일 선택", example = "comfortable")
    @field:NotNull(message = "증상 상태는 필수입니다.")
    val symptomState: SymptomState?,

    @field:Schema(description = "느낀 증상 - 다중 선택. 없으면 빈 배열", example = "[\"throat_foreign_body\"]")
    val symptomTypes: Set<SymptomType> = emptySet(),

    @field:Schema(
        description = "증상 발생 시각 ISO-8601(offset 포함)",
        example = "2026-05-12T14:30:00+09:00",
    )
    @field:NotNull(message = "증상 발생 시각은 필수입니다.")
    @field:ValidOffsetDateTime(message = "증상 발생 시각은 ISO-8601 offset 형식이어야 합니다.")
    val occurredAt: String?,

    @field:Schema(description = "원인 끼니 식별자(UUID). 증상 기록은 식사 연결 필수", example = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
    @field:NotBlank(message = "끼니 식별자는 필수입니다.")
    @field:UUID(message = "끼니 식별자는 UUID 형식이어야 합니다.")
    val mealRecordId: String?,

    @field:Schema(description = "추가 메모 - 최대 200자", nullable = true, example = "속이 메스꺼웠어요")
    @field:Size(max = 200, message = "메모는 200자 이하로 입력해 주세요.")
    val memo: String?,
)
