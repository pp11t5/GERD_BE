package com.gerd.domain.mypage.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.mypage.dto.MedicalInfoUpdateRequestDTO
import com.gerd.domain.mypage.service.MyPageService
import com.gerd.domain.report.service.ReportService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MyPageController(
    private val myPageService: MyPageService,
    private val reportService: ReportService,
) : MyPageApi {

    override fun getMyPageSummary(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<*>> {
        val result = myPageService.getProfileSummary(userDetails.userId)
        return ResponseEntity.ok(ApiResponse.onSuccess(result, CommonSuccessCode.OK))
    }

    override fun getProfile(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<*>> {
        val result = myPageService.getProfile(userDetails.userId)
        return ResponseEntity.ok(ApiResponse.onSuccess(result, CommonSuccessCode.OK))
    }

    override fun getHealthInfo(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<*>> {
        val result = myPageService.getHealthInfo(userDetails.userId)
        return ResponseEntity.ok(ApiResponse.onSuccess(result, CommonSuccessCode.OK))
    }

    override fun updateHealthInfo(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody @Valid request: MedicalInfoUpdateRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>> {
        myPageService.updateHealthInfo(userDetails.userId, request)
        return ResponseEntity.ok(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }

    override fun getReport(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<*>> {
        val result = reportService.getReport(userDetails.userId)
        return ResponseEntity.ok(ApiResponse.onSuccess(result, CommonSuccessCode.OK))  // null이면 data: null
    }
}
