package com.gerd.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class OidcLoginRequestDTO(
    @field:Schema(description = "소셜 SDK에서 발급받은 ID Token", example = "eyJhbGci...")
    @field:NotBlank(message = "ID Token은 필수입니다.")
    val idToken: String,
)
