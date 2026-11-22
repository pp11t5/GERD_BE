package com.gerd.domain.recommendation.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "홈 추천 음식 조회 응답")
data class HomeRecommendationResponseDTO(
    @Schema(description = "취향 프로필 상태. NO_RECORD면 foods가 빈 배열")
    val profileStatus: ProfileStatus,
    @Schema(description = "추천 음식 카드. 최대 4개, 적격 후보가 부족하면 그 수만큼만 내려감")
    val foods: List<RecommendedFoodDTO>,
) {
    @Schema(description = "추천 음식 카드")
    data class RecommendedFoodDTO(
        @Schema(description = "음식 ID. 카드 탭 시 식단 판정 페이지로 전달", example = "101")
        val foodId: Long,
        @Schema(description = "음식명(음식 DB 표기명)", example = "우동")
        val foodName: String,
        @Schema(description = "음식 대표 이미지 URL. 없으면 null", example = "https://cdn.example.com/foods/udon.png")
        val imageUrl: String?,
        @Schema(description = "음식 카테고리명(음식 분류 DB의 표시명)", example = "면류")
        val categoryName: String,
        @Schema(description = "추천 근거 코드")
        val reasonCode: ReasonCode,
        @Schema(description = "카드 보조 라벨. 근거 코드 템플릿으로 생성", example = "비슷한 면류 메뉴")
        val reasonLabel: String,
    )

    @Schema(description = "PERSONALIZED: 식단·증상 기록 기반 / NO_STATE: 식단 기록만 있고 식후 상태 기록 없음 / NO_RECORD: 식단 기록 없음")
    enum class ProfileStatus { PERSONALIZED, NO_STATE, NO_RECORD }

    @Schema(description = "SAME_CATEGORY: 기록한 음식과 동일 카테고리 / SIMILAR_CATEGORY: 사전 매핑된 유사 카테고리 / RECENT_PREFERENCE: 최근 기록 취향과 유사")
    enum class ReasonCode { SAME_CATEGORY, SIMILAR_CATEGORY, RECENT_PREFERENCE }
}
