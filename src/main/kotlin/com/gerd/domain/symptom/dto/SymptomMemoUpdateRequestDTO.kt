package com.gerd.domain.symptom.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class SymptomMemoUpdateRequestDTO(
    @field:Schema(description = "증상 기록 메모 — 최대 200자", nullable = true, example = "속이 메스꺼웠어요")
    @field:Size(max = 200, message = "메모는 200자 이하로 입력해 주세요.")
    val memo: String? = null,
)
