package com.gerd.domain.auth.controller

import com.gerd.domain.auth.dto.AuthTokenResponseDTO
import com.gerd.domain.auth.dto.OidcLoginRequestDTO
import com.gerd.domain.auth.dto.RefreshTokenRequestDTO
import com.gerd.domain.auth.dto.UserMeResponseDTO
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.service.AuthService
import com.gerd.domain.auth.service.OAuthService
import com.gerd.domain.auth.service.WithdrawService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.apiPayload.code.CommonSuccessCode
import com.gerd.domain.auth.security.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 인증 관련 Controller
 * 탈퇴, 로그인, 로그아웃, 토큰 재발급 등을 담당
 */
@RestController
class AuthController(
    private val authService: AuthService,
    private val oAuthService: OAuthService,
    private val withdrawService: WithdrawService,
) : AuthApi {

    override fun getMe(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<UserMeResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(authService.getMe(userDetails.userId), CommonSuccessCode.OK))

    override fun socialLogin(
        @PathVariable provider: String,
        @Valid @RequestBody request: OidcLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>> {
        val authProvider = runCatching { AuthProvider.valueOf(provider.uppercase()) }
            .getOrElse { throw GeneralException(AuthErrorCode.UNSUPPORTED_PROVIDER) }

        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    oAuthService.socialLogin(
                        provider = authProvider,
                        idToken = request.idToken,
                    ),
                    CommonSuccessCode.OK,
                )
            )
    }

    override fun refresh(
        @Valid @RequestBody request: RefreshTokenRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(authService.refresh(request.refreshToken), CommonSuccessCode.OK))

    override fun withdraw(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Unit>> {
        withdrawService.withdraw(userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }

    override fun recoverAccount(
        @PathVariable provider: String,
        @Valid @RequestBody request: OidcLoginRequestDTO,
    ): ResponseEntity<ApiResponse<AuthTokenResponseDTO>> {
        val authProvider = runCatching { AuthProvider.valueOf(provider.uppercase()) }
            .getOrElse { throw GeneralException(AuthErrorCode.UNSUPPORTED_PROVIDER) }

        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    oAuthService.recoverAccount(
                        provider = authProvider,
                        idToken = request.idToken,
                    ),
                    CommonSuccessCode.OK,
                )
            )
    }

    override fun logout(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: RefreshTokenRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.logout(userDetails.userId, request.refreshToken)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }
}
