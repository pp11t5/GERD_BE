package com.gerd.domain.auth.dto

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AppleTokenResponseDTO(
    val accessToken: String,
    val expiresIn: Long,
    val idToken: String,
    val refreshToken: String,
    val tokenType: String,
)
