package com.gerd.domain.auth.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AppleTokenResponseDTO(
    val accessToken: String,
    val expiresIn: Long,
    val idToken: String,
    val refreshToken: String,
    val tokenType: String,
)
