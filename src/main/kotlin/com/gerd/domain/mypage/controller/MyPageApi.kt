package com.gerd.domain.mypage.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.mypage.dto.MedicalInfoResponseDTO
import com.gerd.domain.mypage.dto.MedicalInfoUpdateRequestDTO
import com.gerd.domain.mypage.dto.MyPageSummaryResponseDTO
import com.gerd.domain.mypage.dto.ProfileDetailResponseDTO
import com.gerd.domain.mypage.dto.WeeklyReportResponseDTO
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "MyPage", description = "마이페이지 API")
@RequestMapping("/api/v1/my-page")
interface MyPageApi {

    @Operation(summary = "마이페이지 요약 조회", description = "프로필, 음식 히스토리, 이번 주 기록 요약을 한 번에 조회합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/summary")
    fun getMyPageSummary(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<MyPageSummaryResponseDTO>>

    @Operation(summary = "프로필 정보 조회", description = "닉네임, 이메일, 병명 등 프로필 전체를 조회합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/profile")
    fun getProfile(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<ProfileDetailResponseDTO>>

    @Operation(summary = "알레르기 및 복용약 조회", description = "저장된 알레르기 항목과 복용약 목록을 조회합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/health-info")
    fun getHealthInfo(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<MedicalInfoResponseDTO>>

    @Operation(summary = "알레르기 및 복용약 수정", description = "알레르기 항목과 복용약 목록을 수정합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "수정 성공"))
    @PatchMapping("/health-info")
    fun updateHealthInfo(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody request: MedicalInfoUpdateRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>>


    @Operation(summary = "리포트 상세 조회", description = "현재 날짜 기준 지난주 리포트의 상세 내용을 조회합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/reports")
    fun getReport(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<WeeklyReportResponseDTO?>>
}
