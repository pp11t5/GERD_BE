package com.gerd.domain.recommendation.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.recommendation.dto.HomeRecommendationResponseDTO
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "홈 추천", description = "홈 추천 음식 조회 API")
@RequestMapping("/api/v1/home/recommendations")
interface HomeRecommendationApi {
    @Operation(
        summary = "홈 추천 음식 조회 (명세용 목업 응답)",
        description = """
            홈 "추천 음식" 섹션에 노출할 음식 카드를 내려줍니다. (현재는 FE 연동용 고정 응답)
            - 최대 4개이며, 추천 가능한 음식이 부족하면 있는 만큼만 내려갑니다.
            - 하루 단위로 캐시되며 조회 전용입니다.
            - 카드 탭 시 food_id를 식단 판정 페이지에 전달합니다.

            응답 필드
            - profile_status: PERSONALIZED | NO_STATE(식단 기록만 있음) | NO_RECORD(식단 기록 없음, foods는 빈 배열)
            - category_name: 음식 카테고리명
            - reason_label: 추천 근거 문구. 근거가 없으면 null이며, 이때는 category_name을 보조 라벨로 표시합니다.
        """,
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping
    fun getHomeRecommendations(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<HomeRecommendationResponseDTO>>
}
