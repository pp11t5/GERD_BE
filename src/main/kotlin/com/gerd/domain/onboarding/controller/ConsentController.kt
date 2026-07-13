package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.dto.TermResponseDTO
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

    override fun toggleMarketing(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Boolean>> {
        val agreed = consentService.toggleMarketing(userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(agreed, CommonSuccessCode.OK))
    }

    override fun getTerms(): ResponseEntity<ApiResponse<List<TermResponseDTO>>> {
        val terms = consentService.getTerms().map { TermResponseDTO.from(it) }
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(terms, CommonSuccessCode.OK))
    }

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
