package com.gerd.domain.mypage.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NicknameUpdateRequestDTO(
    @field:Schema(description = "변경할 닉네임", example = "다정한 기린")
    @field:NotBlank(message = "닉네임을 입력해주세요.")
    @field:Size(min = 1, max = 12, message = "닉네임은 12자 이내로 입력해주세요.")
    val nickname: String,
)
