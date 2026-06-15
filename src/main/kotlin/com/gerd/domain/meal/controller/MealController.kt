package com.gerd.domain.meal.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.meal.dto.CreateMealRecordByTextRequestDTO
import com.gerd.domain.meal.dto.CreateMealRecordRequestDTO
import com.gerd.domain.meal.dto.MealGroupDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordSummaryDTO
import com.gerd.domain.meal.dto.UpdateMealMemoRequestDTO
import com.gerd.domain.meal.service.MealRecordCommandService
import com.gerd.domain.meal.service.MealRecordQueryService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MealController(
    private val mealRecordCommandService: MealRecordCommandService,
    private val mealRecordQueryService: MealRecordQueryService,
) : MealApi {

    override fun create(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: CreateMealRecordRequestDTO,
    ): ResponseEntity<ApiResponse<MealRecordSummaryDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    mealRecordCommandService.create(request, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )

    override fun createByText(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: CreateMealRecordByTextRequestDTO,
    ): ResponseEntity<ApiResponse<MealRecordSummaryDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    mealRecordCommandService.createByText(request, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )

    override fun getDetail(
        @CurrentUser userDetails: CustomUserDetails,
        mealId: String,
    ): ResponseEntity<ApiResponse<MealRecordDetailDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    mealRecordQueryService.getDetail(mealId, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )

    override fun updateMemo(
        @CurrentUser userDetails: CustomUserDetails,
        mealId: String,
        @RequestBody request: UpdateMealMemoRequestDTO,
    ): ResponseEntity<ApiResponse<MealRecordDetailDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    mealRecordCommandService.updateMemo(mealId, request, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )

    override fun delete(
        @CurrentUser userDetails: CustomUserDetails,
        mealId: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        mealRecordCommandService.delete(mealId, userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }

    override fun getDaily(
        @CurrentUser userDetails: CustomUserDetails,
        date: String?,
    ): ResponseEntity<ApiResponse<List<MealGroupDTO>>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    mealRecordQueryService.getDaily(date, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )
}
