package com.gerd.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

data class UserMeResponseDTO(
    @field:Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: String,

    @field:Schema(description = "닉네임", example = "홍길동")
    val nickname: String?,

    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:Schema(description = "프로필 사진 URL", example = "https://cdn.example.com/profile.jpg", nullable = true)
    val profileImage: String?,
)
