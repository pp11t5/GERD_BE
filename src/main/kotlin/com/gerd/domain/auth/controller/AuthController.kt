package com.gerd.domain.auth.controller

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.dto.DevLoginRequestDTO
import com.gerd.domain.auth.dto.RefreshTokenRequestDTO
import com.gerd.domain.auth.service.AuthService
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService,
) : AuthApi {

    override fun devLogin(
        @Valid @RequestBody request: DevLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(authService.devLogin(request.email, request.password), CommonSuccessCode.OK))

    override fun refresh(
        @Valid @RequestBody request: RefreshTokenRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(authService.refresh(request.refreshToken), CommonSuccessCode.OK))
}
