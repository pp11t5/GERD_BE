package com.gerd.domain.mypage.dto

import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.onboarding.entity.enums.DiseaseType
import io.swagger.v3.oas.annotations.media.Schema

data class ProfileDetailResponseDTO(
    @field:Schema(description = "닉네임", example = "위장이")
    val nickName: String,
    @field:Schema(description = "프로필 이미지 URL, 없으면 null", nullable = true, example = "https://example.com/image.png")
    val profileImage: String?,
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,
    @field:Schema(description = "소셜 로그인 제공자", example = "KAKAO")
    val provider: AuthProvider,
    @field:Schema(description = "진단 병명", example = "gerd")
    val diseaseType: DiseaseType,
    @field:Schema(description = "대표 건강 정보 1건(알레르기 또는 복용약), 없으면 null", nullable = true, example = "우유")
    val representativeInfo: String?,
    @field:Schema(description = "대표 건강 정보 외 나머지 항목 수, 없으면 0", example = "3")
    val etcCount: Int,
)
