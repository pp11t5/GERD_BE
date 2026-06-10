package com.gerd.domain.fcm.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.fcm.dto.FcmTokenRegisterRequestDTO
import com.gerd.domain.fcm.service.FcmTokenService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * FCM 토큰 등록,삭제 Controller
 */
@RestController
class FcmController(
    private val fcmTokenService: FcmTokenService,
) : FcmApi {

    override fun register(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: FcmTokenRegisterRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>> {
        fcmTokenService.register(userDetails.userId, request)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }

    override fun delete(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Unit>> {
        fcmTokenService.delete(userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }
}
