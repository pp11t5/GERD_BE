package com.gerd.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequestDTO(
    @field:Schema(description = "재발급에 사용할 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9.refresh-token")
    @field:NotBlank(message = "리프레시 토큰은 필수입니다.")
    val refreshToken: String,
)
