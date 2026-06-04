package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.onboarding.dto.OnboardingRequestDTO
import com.gerd.domain.onboarding.dto.OnboardingStatusResponseDTO
import com.gerd.domain.onboarding.service.OnboardingService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OnboardingController(
    private val onboardingService: OnboardingService,
) : OnboardingApi {

    override fun getStatus(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<OnboardingStatusResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(onboardingService.getStatus(userDetails.userId), CommonSuccessCode.OK))

    override fun submit(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: OnboardingRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>> {
        onboardingService.submit(userDetails.userId, request)
        return ResponseEntity
            .status(CommonSuccessCode.CREATED.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.CREATED))
    }
}
