package com.gerd.domain.symptom.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.symptom.dto.SymptomCreateRequestDTO
import com.gerd.domain.symptom.dto.SymptomMemoUpdateRequestDTO
import com.gerd.domain.symptom.dto.SymptomResponseDTO
import com.gerd.domain.symptom.dto.SymptomUpdateRequestDTO
import com.gerd.domain.symptom.exception.SymptomErrorCode
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Symptom", description = "증상 기록 API")
@RequestMapping("/api/v1/symptoms")
interface SymptomApi {

    @Operation(summary = "증상 기록 생성")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "생성 성공"))
    @PostMapping
    fun create(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: SymptomCreateRequestDTO,
    ): ResponseEntity<ApiResponse<SymptomResponseDTO>>

    @Operation(summary = "증상 기록 전체 수정", description = "증상 상태, 증상 목록, 발생 시각, 연결 끼니, 메모를 전체 수정합니다.")
    @ApiErrorExample(SymptomErrorCode::class, "INVALID_DATE_TIME", "SYMPTOM_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "수정 성공"))
    @PutMapping("/{symptomId}")
    fun update(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "증상 기록 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable symptomId: String,
        @Valid @RequestBody request: SymptomUpdateRequestDTO,
    ): ResponseEntity<ApiResponse<Unit?>>

    @Operation(summary = "증상 기록 메모 수정", description = "증상 기록의 메모만 수정합니다.")
    @ApiErrorExample(SymptomErrorCode::class, "SYMPTOM_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "수정 성공"))
    @PatchMapping("/{symptomId}/memo")
    fun updateMemo(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "증상 기록 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable symptomId: String,
        @Valid @RequestBody request: SymptomMemoUpdateRequestDTO,
    ): ResponseEntity<ApiResponse<Unit?>>

    @Operation(summary = "증상 기록 단건 조회", description = "증상 기록 상세 정보를 조회합니다.")
    @ApiErrorExample(SymptomErrorCode::class, "SYMPTOM_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/{symptomId}")
    fun getDetail(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "증상 기록 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable symptomId: String,
    ): ResponseEntity<ApiResponse<SymptomResponseDTO>>

    @Operation(summary = "증상 기록 삭제", description = "증상 기록을 삭제합니다.")
    @ApiErrorExample(SymptomErrorCode::class, "SYMPTOM_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "삭제 성공"))
    @DeleteMapping("/{symptomId}")
    fun delete(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "증상 기록 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable symptomId: String,
    ): ResponseEntity<ApiResponse<Unit?>>

}
