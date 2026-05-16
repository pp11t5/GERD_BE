package com.gerd.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class DevLoginRequestDTO(
    @field:Schema(description = "로그인에 사용할 이메일 주소", example = "user@test.com")
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,

    @field:Schema(description = "로그인에 사용할 비밀번호", example = "dev1234!")
    @field:NotBlank(message = "비밀번호는 필수입니다.")
    val password: String,
)
