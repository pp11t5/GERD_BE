package com.gerd.domain.dictionary.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.dictionary.dto.DictionaryCountResponseDTO
import com.gerd.domain.dictionary.dto.DictionaryCautionRiskResponseDTO
import com.gerd.domain.dictionary.dto.DictionarySafeResponseDTO
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Dictionary", description = "도감 API")
@RequestMapping("/api/v1/dictionary")
interface DictionaryApi {

    @Operation(summary = "도감 탭 카운트 조회", description = "안전 음식 수와 주의·위험 음식 수를 함께 반환합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/count")
    fun getCount(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<DictionaryCountResponseDTO>>

    @Operation(
        summary = "안전 음식 목록 조회",
        description = "먹고 편안했던 음식 목록을 최신순으로 반환합니다(커서 페이징, 기본 20·최대 20) cursor 미지정 시 첫 페이지입니다.",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/safe")
    fun getSafeFoods(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "이전 응답의 nextCursor(첫 페이지는 생략)", example = "120")
        @RequestParam(required = false) @Min(0) @Max(30) cursor: Long?,
        @Parameter(description = "페이지 크기(기본 20, 최대 20)", example = "20")
        @RequestParam(required = false) @Min(1) @Max(20) size: Int?,
    ): ResponseEntity<ApiResponse<DictionarySafeResponseDTO>>

    @Operation(
        summary = "주의·위험 음식 목록 조회",
        description = "신호등 판정에서 주의·위험 등급을 받은 음식 목록을 최신순으로 반환합니다(커서 페이징, 기본 20·최대 20) cursor 미지정 시 첫 페이지입니다.",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/caution-risk")
    fun getCautionRiskFoods(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "이전 응답의 nextCursor(첫 페이지는 생략)", example = "120")
        @RequestParam(required = false) @Min(0) @Max(30) cursor: Long?,
        @Parameter(description = "페이지 크기(기본 20, 최대 20)", example = "20")
        @RequestParam(required = false) @Min(1) @Max(20) size: Int?,
    ): ResponseEntity<ApiResponse<DictionaryCautionRiskResponseDTO>>
}
