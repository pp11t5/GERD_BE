package com.gerd.domain.food.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.food.dto.AddRecentRequestDTO
import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.dto.RecentFoodDTO
import com.gerd.domain.food.service.FoodSearchService
import com.gerd.domain.food.service.RecentFoodService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FoodController(
    private val foodSearchService: FoodSearchService,
    private val recentFoodService: RecentFoodService,
) : FoodApi {

    override fun search(
        @CurrentUser userDetails: CustomUserDetails,
        q: String?,
        size: Int?,
    ): ResponseEntity<ApiResponse<List<FoodSummaryDTO>>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(foodSearchService.search(q, size, userDetails.userId), CommonSuccessCode.OK))

    override fun getRecent(
        @CurrentUser userDetails: CustomUserDetails,
        size: Int?,
    ): ResponseEntity<ApiResponse<List<RecentFoodDTO>>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(recentFoodService.getRecent(size, userDetails.userId), CommonSuccessCode.OK))

    override fun addRecent(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: AddRecentRequestDTO,
    ): ResponseEntity<ApiResponse<RecentFoodDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    recentFoodService.addRecent(request.foodExternalId, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )

    override fun deleteRecent(
        @CurrentUser userDetails: CustomUserDetails,
        foodExternalId: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        recentFoodService.deleteRecent(foodExternalId, userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }

    override fun deleteAllRecent(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Unit>> {
        recentFoodService.deleteAllRecent(userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }
}
