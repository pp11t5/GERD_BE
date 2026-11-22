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
            최근 식단 기록으로 취향을 추정해 홈 "추천 음식" 섹션에 노출할 음식 카드를 내려줍니다.
            현재는 FE 연동을 위한 명세용 목업이며 고정 응답을 반환합니다.

            노출 규칙
            - 최대 4개, 가로 스크롤 카드. 적격 후보가 4개 미만이면 있는 만큼만 내려가며 수를 채우지 않습니다.
            - 하루 단위 캐시: 하루에 한 번 계산한 결과를 같은 날 재사용합니다.
            - 조회 전용: 호출이나 카드 탭만으로 식단 기록이 생성되지 않습니다.
            - 카드 탭 시 food_id를 식단 판정 페이지에 전달합니다.

            응답 필드
            - profile_status: PERSONALIZED(식단·상태 기록 기반) | NO_STATE(식단 기록만 있음) | NO_RECORD(식단 기록 없음, foods는 빈 배열이며 FE는 "식단 기록이 없어요." 표시)
            - food_name: 음식 DB 표기명
            - category_name: 음식 분류 DB의 카테고리 표시명
            - reason_label: 추천 근거 문구. 확인 가능한 근거가 있을 때만 내려가며 없으면 null입니다. null이면 FE는 category_name을 보조 라벨로 표시합니다.

            추천 근거(reason_label)와 유사 음식 매핑 기준
            - 동일 카테고리: 사용자가 기록한 음식과 같은 카테고리 후보. 가장 먼저 탐색합니다. 예: "비슷한 면류 메뉴"
            - 유사 카테고리: 후보가 부족할 때 음식 DB에 사전 연결된 유사 카테고리로 확장한 후보. 예: 국물 요리 ↔ 국수·탕·찌개류
            - 최근 기록 취향: 카테고리가 달라도 재료·조리 방식 등 속성이 최근 기록 취향과 가까워 순위가 오른 후보
            - 위 근거로 문구를 만들 수 없으면 reason_label은 null입니다.

            제외 규칙
            - 등록 알레르기와 매칭되는 음식은 후보에서 제외됩니다.
            - 기록한 음식과 동일한 음식, 중복 후보는 제외됩니다.
            - 트리거와 식후 불편 기록은 순위를 낮추는 데만 사용하며, 안전을 보장하는 표현은 쓰지 않습니다.
        """,
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping
    fun getHomeRecommendations(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<HomeRecommendationResponseDTO>>
}
