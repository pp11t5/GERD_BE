package com.gerd.domain.recommendation.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.recommendation.dto.HomeRecommendationResponseDTO
import com.gerd.domain.recommendation.dto.HomeRecommendationResponseDTO.ProfileStatus
import com.gerd.domain.recommendation.dto.HomeRecommendationResponseDTO.ReasonCode
import com.gerd.domain.recommendation.dto.HomeRecommendationResponseDTO.RecommendedFoodDTO
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class HomeRecommendationController : HomeRecommendationApi {
    override fun getHomeRecommendations(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<HomeRecommendationResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(SAMPLE_RESPONSE))

    companion object {
        private val SAMPLE_RESPONSE =
            HomeRecommendationResponseDTO(
                profileStatus = ProfileStatus.PERSONALIZED,
                foods =
                    listOf(
                        RecommendedFoodDTO(101L, "우동", null, "면류", ReasonCode.SAME_CATEGORY, "비슷한 면류 메뉴"),
                        RecommendedFoodDTO(102L, "순두부찌개", null, "찌개·탕류", ReasonCode.SIMILAR_CATEGORY, "비슷한 한식 메뉴"),
                        RecommendedFoodDTO(103L, "닭죽", null, "밥·죽류", ReasonCode.RECENT_PREFERENCE, "최근 드신 음식과 비슷해요"),
                        RecommendedFoodDTO(104L, "잔치국수", null, "면류", ReasonCode.SAME_CATEGORY, "비슷한 면류 메뉴"),
                    ),
            )
    }
}
