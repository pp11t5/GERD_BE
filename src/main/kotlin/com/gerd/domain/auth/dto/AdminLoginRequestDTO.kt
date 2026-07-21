package com.gerd.domain.auth.dto

import jakarta.validation.constraints.NotBlank

data class AdminLoginRequestDTO(
    @field:NotBlank val email: String,
    @field:NotBlank val secret: String,
)
