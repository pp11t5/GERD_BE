package com.gerd.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class DevLoginRequestDTO(
    @field:Schema(description = "로그인에 사용할 닉네임", example = "dev-user")
    @field:NotBlank(message = "닉네임은 필수입니다.")
    val nickname: String,
)
