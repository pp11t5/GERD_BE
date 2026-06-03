package com.gerd.domain.onboarding.dto

import io.swagger.v3.oas.annotations.media.Schema

data class OnboardingStatusResponseDTO(
    @field:Schema(description = "온보딩 완료 여부 (user_profiles row 존재)", example = "true")
    val onboarded: Boolean,
)
