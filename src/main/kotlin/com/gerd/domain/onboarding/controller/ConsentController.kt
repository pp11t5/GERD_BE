package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.service.ConsentService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ConsentController(
    private val consentService: ConsentService,
) : ConsentApi {

    override fun submitConsent(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: ConsentRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>> {
        consentService.submitConsent(userDetails.userId, request)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }
}
