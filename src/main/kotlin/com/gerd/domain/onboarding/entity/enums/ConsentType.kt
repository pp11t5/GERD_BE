package com.gerd.domain.onboarding.entity.enums

// 약관동의 03 type (user_consents.consent_type). required=필수 동의 여부 — 필수 3종 미동의 시 온보딩 진입 거부
enum class ConsentType(val code: String, val required: Boolean) {
    TOS("tos", true),
    PRIVACY("privacy", true),
    HEALTH_SENSITIVE("health_sensitive", true),
    MARKETING("marketing", false),
    ;

    companion object {
        fun from(code: String): ConsentType =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown ConsentType code: $code")
    }
}
