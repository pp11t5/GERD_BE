package com.gerd.domain.auth.controller

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.dto.OidcLoginRequestDTO
import com.gerd.domain.auth.dto.RefreshTokenRequestDTO
import com.gerd.domain.auth.dto.UserMeResponseDTO
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.domain.auth.security.CustomUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Auth", description = "인증 API")
@RequestMapping("/api/v1/auth")
interface AuthApi {

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 기본 정보를 조회합니다.")
    @ApiErrorExample(AuthErrorCode::class, "USER_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/me")
    fun getMe(@CurrentUser userDetails: CustomUserDetails): ResponseEntity<ApiResponse<UserMeResponseDTO>>

    @Operation(
        summary = "소셜 로그인",
        description = """
            소셜 SDK에서 발급받은 ID Token으로 로그인합니다.
            - provider: kakao
            - 기존 가입자: 바로 토큰 발급
            - 신규 사용자: 자동 가입 후 토큰 발급
            - 이메일·닉네임 제공 동의가 필요합니다.

            [에러 분기]
            - ACCOUNT_RECOVERABLE(AUTH403_5): 탈퇴 유예기간 중 → POST /{provider}/recover 호출
        """,
    )
    @ApiErrorExample(
        AuthErrorCode::class,
        "INVALID_TOKEN",
        "EXPIRED_TOKEN",
        "EMAIL_REQUIRED",
        "NICKNAME_REQUIRED",
        "UNSUPPORTED_PROVIDER",
        "ACCOUNT_INACTIVE",
        "ACCOUNT_RECOVERABLE",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "토큰 발급 성공"))
    @PostMapping("/{provider}/login")
    fun socialLogin(
        @PathVariable provider: String,
        @Valid @RequestBody request: OidcLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>>

    @Operation(
        summary = "토큰 재발급",
        description = """
            Refresh Token으로 액세스 토큰과 리프레시 토큰을 재발급합니다.
            - 유효하지 않거나 만료된 Refresh Token이면 예외를 반환합니다.
        """,
    )
    @ApiErrorExample(AuthErrorCode::class, "INVALID_TOKEN", "EXPIRED_TOKEN", "USER_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "토큰 재발급 성공"))
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>>

    @Operation(
        summary = "회원 탈퇴",
        description = """
            현재 로그인한 계정을 탈퇴 처리합니다.
            - 계정 상태를 DELETED로 변경하여 탈퇴 유예기간(14일) 동안 복구 가능하도록 합니다.
            - 14일이 지나야 계정이 완전 삭제 처리되며 카카오 계정도 연결 해제됩니다.
        """,
    )
    @ApiErrorExample(AuthErrorCode::class, "USER_NOT_FOUND", "KAKAO_UNLINK_FAILED")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "탈퇴 성공"))
    @DeleteMapping("/withdraw")
    fun withdraw(@CurrentUser userDetails: CustomUserDetails): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "로그아웃",
        description = """
            현재 로그인한 기기의 Refresh Token을 만료 처리하여 로그아웃합니다.
            - Redis에서 해당 토큰을 삭제하여 더 이상 사용할 수 없도록 합니다.
        """,
    )
    @ApiErrorExample(AuthErrorCode::class, "USER_NOT_FOUND", "INVALID_TOKEN")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "로그아웃 성공"))
    @DeleteMapping("/logout")
    fun logout(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: RefreshTokenRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "계정 복구",
        description = """
            탈퇴 유예기간(14일) 중인 계정을 복구합니다.
            - id_token: 로그인 시도 시 받은 idToken을 그대로 전달합니다.
            - provider : 현재 `kakao` 고정, 확장성을 위해 path variable로 받습니다.
            - 복구 성공 시 토큰을 발급합니다.
        """,
    )
    @ApiErrorExample(AuthErrorCode::class, "USER_NOT_FOUND", "INVALID_TOKEN")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "복구 및 토큰 발급 성공"))
    @PostMapping("/{provider}/recover")
    fun recoverAccount(
        @PathVariable provider: String,
        @Valid @RequestBody request: OidcLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>>

}
