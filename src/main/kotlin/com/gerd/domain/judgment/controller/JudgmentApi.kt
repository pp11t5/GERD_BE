package com.gerd.domain.judgment.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.dto.TextJudgmentResponseDTO
import com.gerd.domain.symptom.dto.FoodSymptomResponseDTO
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
            사용자 건강 컨텍스트(트리거·알레르기·증상)를 반영해 음식의 신호등 등급과 개인화 분석을 반환합니다.
            - grade: RECOMMEND(🟢) | CAUTION(🟡) | RISK(🔴) | UNKNOWN(⚪, 검색으로 새로 등록된 음식이거나 LLM 판정 실패 시)
            - items: 항상 2슬롯 — [0]=트리거·증상 분석, [1]=최근 증상 기반 상세 분석
            - substitutes: CAUTION/RISK일 때만 대체 식단 노출(없으면 빈 배열). 사용자가 등록한 트리거·알레르기 성분을 가진 음식은 제외됩니다.
            - 검색어와 완전 일치하지 않아 새로 생성된 음식(source=USER)은 판정 근거가 없어 UNKNOWN으로 반환됩니다. 관리자가 승격하면 이후부터 정식 판정이 제공됩니다.
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
        summary = "음식 이름으로 신호등 판정",
        description = """
            검색 결과에 없는 음식을 직접 입력했을 때 사용합니다.
            DB 음식 엔티티를 생성하지 않고 LLM으로 즉시 판정하며, 대체식단은 항상 빈 배열로 반환됩니다.
            음식이 추후 식사 기록에 등록되면 그 시점에 DB에 저장됩니다.
            - grade: RECOMMEND(🟢) | CAUTION(🟡) | RISK(🔴) | UNKNOWN(⚪, LLM 판정 실패 시)
            - 동일한 (이름 × 사용자 상태)는 24시간 캐시됩니다.
        """,
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "판정 성공"))
    @GetMapping("/judgment")
    fun getJudgmentByText(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "판정할 음식 이름", example = "치킨")
        @RequestParam name: String,
    ): ResponseEntity<ApiResponse<TextJudgmentResponseDTO>>

    @Operation(
        summary = "음식 ID로 연결 증상 목록 조회",
        description = """
            해당 음식을 포함한 식사에 연결된 증상 기록을 최신순으로 조회합니다.
            신호등 상세 화면의 증상 기록 전체보기와 증상 상세 진입에 사용합니다.
            symptomId로 GET /api/v1/symptoms/{symptomId}를 호출하면 증상 상세를 조회할 수 있습니다.
        """,
    )
    @ApiErrorExample(FoodErrorCode::class, "FOOD_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"))
    @GetMapping("/{foodExternalId}/symptoms")
    fun getSymptomsByFoodId(
        @CurrentUser userDetails: CustomUserDetails,
        @Parameter(description = "음식 외부 식별자(UUID)", example = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")
        @PathVariable foodExternalId: String,
    ): ResponseEntity<ApiResponse<List<FoodSymptomResponseDTO>>>

}
