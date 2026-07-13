package com.gerd.domain.onboarding.dto

import com.gerd.domain.onboarding.entity.Term
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class TermResponseDTO(
    @field:Schema(description = "약관 ID", example = "1")
    val id: Long,

    @field:Schema(description = "약관 코드", example = "tos")
    val code: String,

    @field:Schema(description = "버전", example = "1.0")
    val version: String,

    @field:Schema(description = "약관 제목", example = "서비스 이용약관")
    val title: String,

    @field:Schema(description = "약관 본문")
    val content: String,

    @field:Schema(description = "필수 동의 여부", example = "true")
    val required: Boolean,

    @field:Schema(description = "시행일 (ISO-8601)", example = "2026-01-01")
    val effectiveDate: LocalDate,
) {
    companion object {
        fun from(term: Term) = TermResponseDTO(
            id = term.id,
            code = term.code,
            version = term.version,
            title = term.title,
            content = term.content,
            required = term.required,
            effectiveDate = term.effectiveDate,
        )
    }
}
