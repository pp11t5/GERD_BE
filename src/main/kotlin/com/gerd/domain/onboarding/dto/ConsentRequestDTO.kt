package com.gerd.domain.onboarding.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ConsentRequestDTO(
    @field:Schema(description = "약관별 동의 목록")
    val consents: List<ConsentItem>,
) {
    data class ConsentItem(
        @field:Schema(description = "약관 ID", example = "1")
        val termId: Long,

        @field:Schema(description = "동의 여부", example = "true")
        val agreed: Boolean,
    )
}
