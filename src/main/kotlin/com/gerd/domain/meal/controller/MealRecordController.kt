package com.gerd.domain.meal.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.meal.dto.MealCandidatesDTO
import com.gerd.domain.meal.dto.MealFoodRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordByIDRequestDTO
import com.gerd.domain.meal.dto.MealRecordTextRequestDTO
import com.gerd.domain.meal.service.MealCommandService
import com.gerd.domain.meal.service.MealQueryService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MealRecordController(
    private val mealCommandService: MealCommandService,
    private val mealQueryService: MealQueryService,
) : MealRecordApi {

    override fun createNew(
        @CurrentUser userDetails: CustomUserDetails,
        @PathVariable foodId: String,
        @Valid @RequestBody(required = false) request: MealRecordByIDRequestDTO?,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(mealCommandService.createNew(foodId, request?.eatenAt, userDetails.userId), CommonSuccessCode.OK))

    override fun createNewByText(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: MealRecordTextRequestDTO,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(mealCommandService.createNewByText(request.name, request.eatenAt, userDetails.userId), CommonSuccessCode.OK))

    override fun append(
        @CurrentUser userDetails: CustomUserDetails,
        @PathVariable mealRecordId: String,
        @PathVariable foodId: String,
        @Valid @RequestBody(required = false) request: MealRecordByIDRequestDTO?,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(mealCommandService.append(mealRecordId, foodId, request?.eatenAt, userDetails.userId), CommonSuccessCode.OK))

    override fun appendByText(
        @CurrentUser userDetails: CustomUserDetails,
        @PathVariable mealRecordId: String,
        @Valid @RequestBody request: MealRecordTextRequestDTO,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(mealCommandService.appendByText(mealRecordId, request.name, request.eatenAt, userDetails.userId), CommonSuccessCode.OK))

    override fun getDetail(
        @CurrentUser userDetails: CustomUserDetails,
        mealFoodId: String,
    ): ResponseEntity<ApiResponse<MealFoodRecordDetailDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(mealQueryService.getDetail(mealFoodId, userDetails.userId), CommonSuccessCode.OK))

    override fun delete(
        @CurrentUser userDetails: CustomUserDetails,
        mealFoodId: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        mealCommandService.delete(mealFoodId, userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }

    override fun getCandidates(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<List<MealCandidatesDTO>>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(mealQueryService.getCandidates(userDetails.userId), CommonSuccessCode.OK))

    override fun getGroupDetail(
        @CurrentUser userDetails: CustomUserDetails,
        mealRecordId: String,
    ): ResponseEntity<ApiResponse<MealRecordDetailDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(mealQueryService.getGroupDetail(mealRecordId, userDetails.userId), CommonSuccessCode.OK))

    override fun deleteMealRecord(
        @CurrentUser userDetails: CustomUserDetails,
        mealRecordId: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        mealCommandService.deleteMealRecord(mealRecordId, userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }
}
