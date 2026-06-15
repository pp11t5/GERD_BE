package com.gerd.domain.meal.dto

import io.swagger.v3.oas.annotations.media.Schema

// 식사 기록 메모 수정 — "추가 메모 기록". null/빈 문자열 = 메모 삭제 (D7). 200자 초과는 서비스에서 MEAL400_1
data class UpdateMealMemoRequestDTO(
    @field:Schema(description = "추가 메모 (0~200자). null/빈 문자열은 메모 삭제", example = "점심 후 잠깐 누웠더니 답답했음", nullable = true)
    val memo: String?,
)
