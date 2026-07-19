package com.gerd.domain.auth.controller

import com.gerd.domain.auth.dto.AdminLoginRequestDTO
import com.gerd.domain.auth.dto.AdminLoginResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Admin Auth", description = "관리자 인증 API")
@RequestMapping("/api/v1/auth/admin")
interface AdminAuthApi {

    @Operation(summary = "어드민 로그인", description = "이메일과 시크릿으로 어드민 액세스 토큰을 발급합니다.")
    @ApiErrorExample(AuthErrorCode::class, "INVALID_ADMIN_CREDENTIALS")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "토큰 발급 성공"))
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AdminLoginResponseDTO>>
}