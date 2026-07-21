package com.gerd.domain.auth.controller

import com.gerd.domain.auth.dto.AdminLoginRequestDTO
import com.gerd.domain.auth.dto.AdminLoginResponseDTO
import com.gerd.domain.auth.service.AdminAuthService
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminAuthController(
    private val adminAuthService: AdminAuthService,
) : AdminAuthApi {

    override fun login(
        @Valid @RequestBody request: AdminLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AdminLoginResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(adminAuthService.login(request.email, request.secret), CommonSuccessCode.OK))
}
