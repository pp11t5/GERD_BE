package com.gerd.domain.auth.dto

import com.gerd.domain.auth.entity.enums.UserRole
import io.swagger.v3.oas.annotations.media.Schema

data class AuthTokenResponseDTO(
    @field:Schema(description = "발급된 Access Token", example = "eyJhbGciOiJIUzI1NiJ9.access-token")
    val accessToken: String,

    @field:Schema(description = "발급된 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9.refresh-token")
    val refreshToken: String,

    @field:Schema(description = "토큰이 발급된 사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: String,

    @field:Schema(description = "토큰이 발급된 사용자 이메일", example = "user@test.com")
    val email: String,

    @field:Schema(description = "토큰이 발급된 사용자의 권한", example = "USER")
    val role: UserRole,
)
