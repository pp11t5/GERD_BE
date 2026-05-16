package com.gerd.domain.auth.controller

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.dto.DevLoginRequestDTO
import com.gerd.domain.auth.dto.RefreshTokenRequestDTO
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

@Tag(name = "Auth", description = "인증 API")
@RequestMapping("/api/v1/auth")
interface AuthApi {

    @Operation(
        summary = "개발용 로그인",
        description = """
            이메일과 비밀번호를 사용해 액세스 토큰과 리프레시 토큰을 발급합니다.
            - body의 `email`, `password` 값을 사용합니다.
            - `email`로 사용자를 조회한 뒤 `password`가 저장된 비밀번호와 일치하는지 검증합니다.
            - 인증에 성공하면 액세스 토큰과 리프레시 토큰을 함께 반환합니다.
            - 존재하지 않는 이메일이면 예외를 반환합니다.
            - 비밀번호가 일치하지 않으면 인증 실패 예외를 반환합니다.
            - 현재 엔드포인트는 개발 환경 편의를 위한 용도이며 운영 환경에서는 비활성화 예정입니다.
        """,
    )
    @ApiErrorExample(
        AuthErrorCode.USER_NOT_FOUND,
        AuthErrorCode.INVALID_PASSWORD,
    )
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "토큰 발급 성공"),
    )
    @PostMapping("/dev-login")
    fun devLogin(
        @Valid @RequestBody request: DevLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>>

    @Operation(
        summary = "토큰 재발급",
        description = """
            Refresh Token을 사용해 액세스 토큰과 리프레시 토큰을 재발급합니다.
            - body의 `refreshToken` 값을 사용합니다.
            - 전달된 Refresh Token의 유효성 및 만료 여부를 먼저 검증합니다.
            - 토큰이 유효하면 토큰에서 사용자 ID를 추출한 뒤 해당 사용자를 조회합니다.
            - 재발급에 성공하면 새 액세스 토큰과 새 리프레시 토큰을 함께 반환합니다.
            - 유효하지 않거나 만료된 Refresh Token이면 예외를 반환합니다.
            - 토큰의 사용자와 일치하는 회원이 없으면 예외를 반환합니다.
            - 현재 구현은 `refreshToken` 값만 사용하며 `deviceId` 검증이나 서버 저장소 기반 Refresh Token Rotation 무효화는 포함하지 않습니다.
        """,
    )
    @ApiErrorExample(
        AuthErrorCode.INVALID_TOKEN,
        AuthErrorCode.EXPIRED_TOKEN,
        AuthErrorCode.USER_NOT_FOUND,
    )
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "토큰 재발급 성공"),
    )
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>>
}
