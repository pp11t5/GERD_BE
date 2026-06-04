package com.gerd.domain.auth.controller

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.dto.DevLoginRequestDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.service.AuthService
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발용 로그인
 * prod 환경에서는 비활성화
 * 추후 삭제 고려
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@Profile("!prod")
class DevAuthController(
    private val authService: AuthService,
) {

    @Operation(
        summary = "개발용 로그인",
        description = """
            닉네임으로 액세스 토큰과 리프레시 토큰을 발급합니다.
            - 닉네임으로 사전에 시드된 Mock 사용자를 조회합니다.
            - 운영 환경(prod profile)에서는 비활성화됩니다.
        """,
    )
    @ApiErrorExample(AuthErrorCode::class, "USER_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "토큰 발급 성공"))
    @PostMapping("/dev-login")
    fun devLogin(
        @Valid @RequestBody request: DevLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(authService.devLogin(request.nickname), CommonSuccessCode.OK))
}
