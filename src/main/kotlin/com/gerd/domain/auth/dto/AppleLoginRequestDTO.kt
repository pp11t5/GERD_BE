package com.gerd.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class AppleLoginRequestDTO(
    @field:Schema(
        description = "Apple 로그인에서 발급받은 Authorization Code",
        example = "c123...",
    )
    @field:NotBlank(message = "Authorization Code는 필수입니다.")
    val authorizationCode: String,

    @field:Schema(
        description = "Apple 인증 요청에 전달한 nonce와 동일한 값",
        example = "bd82a51d-31e8-4f68-a9d9-8f65c29745f2",
    )
    @field:NotBlank(message = "nonce는 필수입니다.")
    val nonce: String,
)
