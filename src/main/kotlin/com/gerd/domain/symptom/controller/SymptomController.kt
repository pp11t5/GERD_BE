package com.gerd.domain.symptom.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.symptom.dto.SymptomCreateRequestDTO
import com.gerd.domain.symptom.dto.SymptomMemoUpdateRequestDTO
import com.gerd.domain.symptom.dto.SymptomResponseDTO
import com.gerd.domain.symptom.dto.SymptomUpdateRequestDTO
import com.gerd.domain.symptom.service.SymptomService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SymptomController(
    private val symptomService: SymptomService,
) : SymptomApi {

    override fun create(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: SymptomCreateRequestDTO,
    ): ResponseEntity<ApiResponse<SymptomResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(symptomService.create(userDetails.userId, request), CommonSuccessCode.OK))

    override fun getDetail(
        @CurrentUser userDetails: CustomUserDetails,
        @PathVariable symptomId: String,
    ): ResponseEntity<ApiResponse<SymptomResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(symptomService.getDetail(symptomId, userDetails.userId), CommonSuccessCode.OK))

    override fun update(
        @CurrentUser userDetails: CustomUserDetails,
        @PathVariable symptomId: String,
        @Valid @RequestBody request: SymptomUpdateRequestDTO,
    ): ResponseEntity<ApiResponse<Unit?>> {
        symptomService.update(symptomId, request, userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess())
    }

    override fun updateMemo(
        @CurrentUser userDetails: CustomUserDetails,
        @PathVariable symptomId: String,
        @Valid @RequestBody request: SymptomMemoUpdateRequestDTO,
    ): ResponseEntity<ApiResponse<Unit?>> {
        symptomService.updateMemo(symptomId, request, userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess())
    }

    override fun delete(
        @CurrentUser userDetails: CustomUserDetails,
        @PathVariable symptomId: String,
    ): ResponseEntity<ApiResponse<Unit?>> {
        symptomService.delete(symptomId, userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess())
    }
}
