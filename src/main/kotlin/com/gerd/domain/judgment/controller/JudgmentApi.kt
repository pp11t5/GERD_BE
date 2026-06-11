package com.gerd.domain.judgment.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Judgment", description = "음식 신호등 판정 API")
@RequestMapping("/api/v1/foods")
interface JudgmentApi {

    @Operation(
        summary = "음식 신호등 판정",
        description = """
            사용자 건강 컨텍스트(트리거·알레르기·복용약·증상)를 반영해 음식의 신호등 등급과 개인화 분석을 반환합니다.
            - grade: RECOMMEND(🟢) | CAUTION(🟡) | RISK(🔴) | UNKNOWN(⚪)
            - items: 항상 2슬롯 — [0]=트리거·증상 분석, [1]=알레르기·복용약 분석
            - substitutes: CAUTION/RISK일 때만 대체 식단 노출(없으면 빈 배열). 사용자가 등록한 트리거·알레르기 성분을 가진 음식은 제외됩니다.
            - 본인이 직접 추가한 음식(source=user)은 검수 정보가 없어 항상 UNKNOWN입니다.
            - 동일한 (음식 × 사용자 상태)는 24시간 캐시돼 재호출 없이 재사용됩니다(cached=true).
        """,
    )
    @ApiErrorExample(FoodErrorCode::class, "FOOD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "판정 성공"))
    @GetMapping("/{foodExternalId}/judgment")
    fun getJudgment(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable foodExternalId: String,
    ): ResponseEntity<ApiResponse<JudgmentResponseDTO>>
}
