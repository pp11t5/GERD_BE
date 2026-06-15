package com.gerd.domain.judgment.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.judgment.dto.JudgmentResponseDTO

import com.gerd.domain.judgment.dto.TextJudgmentResponseDTO
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
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "음식 판정", description = "음식 신호등 판정 API")
@RequestMapping("/api/v1/foods")
interface JudgmentApi {

    @Operation(
        summary = "음식 ID로 음식 신호등 판정",
        description = """
            사용자 건강 컨텍스트(트리거·알레르기·복용약·증상)를 반영해 음식의 신호등 등급과 개인화 분석을 반환합니다.
            - grade: RECOMMEND(🟢) | CAUTION(🟡) | RISK(🔴) | UNKNOWN(⚪)
            - items: 항상 2슬롯 — [0]=트리거·증상 분석, [1]=알레르기·복용약 분석
            - substitutes: CAUTION/RISK일 때만 대체 식단 노출(없으면 빈 배열). 사용자가 등록한 트리거·알레르기 성분을 가진 음식은 제외됩니다.
            - 본인이 직접 추가한 음식(source=user)은 검수 정보가 없어 항상 UNKNOWN입니다.
            - 동일한 (음식 × 사용자 상태)는 24시간 캐시됩니다. 캐시 히트 여부는 X-Cache: HIT/MISS 헤더로 확인할 수 있습니다.
            - 증상 기록은 최대 3개까지 미리보기 표시됩니다. 
        """,
    )
    @ApiErrorExample(FoodErrorCode::class, "FOOD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "판정 성공"))
    @GetMapping("/{foodExternalId}/judgment")
    fun getJudgmentById(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable foodExternalId: String,
    ): ResponseEntity<ApiResponse<JudgmentResponseDTO>>

    @Operation(
        summary = "음식 텍스트로 신호등 판정",
        description = """
            찾으려는 음식이 검색 결과에 없는 경우, 음식 이름을 자유 텍스트로 입력받아 신호등 등급과 개인화 분석을 반환합니다.
            - DB에 등록되지 않은 음식도 판정 가능합니다.
            - grade: RECOMMEND(🟢) | CAUTION(🟡) | RISK(🔴) | UNKNOWN(⚪)
            - items: 항상 2슬롯 — [0]=트리거·증상 분석, [1]=알레르기·복용약 분석 (LLM이 알레르기 포함 여부를 추론)
            - substitutes: 텍스트 입력은 DB 음식 엔티티가 없어 항상 빈 배열입니다.
            - 동일한 (음식명 × 사용자 상태)는 24시간 캐시됩니다. 캐시 히트 여부는 X-Cache: HIT/MISS 헤더로 확인할 수 있습니다.
            - 증상 기록은 최대 3개까지 미리보기 표시됩니다. 
        """,
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "판정 성공"))
    @GetMapping("/judgment")
    fun getJudgmentByText(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestParam foodTextInput: String,
    ): ResponseEntity<ApiResponse<TextJudgmentResponseDTO>>

}
