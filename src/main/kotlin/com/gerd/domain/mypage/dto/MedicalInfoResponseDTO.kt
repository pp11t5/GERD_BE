package com.gerd.domain.mypage.dto

import io.swagger.v3.oas.annotations.media.Schema

data class MedicalInfoResponseDTO(
    @field:Schema(description = "등록된 알레르기 이름 목록", example = "[\"우유\", \"땅콩\"]")
    val allergies: List<String>,
)
