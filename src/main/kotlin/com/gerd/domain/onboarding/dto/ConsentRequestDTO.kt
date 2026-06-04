package com.gerd.domain.onboarding.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ConsentRequestDTO(
    @field:Schema(description = "[필수] 서비스 이용약관 동의", example = "true")
    val tos: Boolean,

    @field:Schema(description = "[필수] 개인정보 수집·이용 동의", example = "true")
    val privacy: Boolean,

    @field:Schema(description = "[필수] 민감정보(건강) 수집 동의", example = "true")
    val healthSensitive: Boolean,

    @field:Schema(description = "[선택] 마케팅·푸시 알림 동의", example = "false")
    val marketing: Boolean,
)
