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
        @Schema(description = "음식 카테고리명(음식 분류 DB의 표시명)", example = "면류")
        val categoryName: String,
        @Schema(description = "추천 근거 라벨. 확인 가능한 근거가 없으면 null이며 FE는 category_name을 보조 라벨로 표시", example = "비슷한 면류 메뉴")
        val reasonLabel: String?,
    )

    @Schema(description = "PERSONALIZED: 식단·증상 기록 기반 / NO_STATE: 식단 기록만 있고 식후 상태 기록 없음 / NO_RECORD: 식단 기록 없음")
    enum class ProfileStatus { PERSONALIZED, NO_STATE, NO_RECORD }
}
