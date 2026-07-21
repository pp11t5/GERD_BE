package com.gerd.domain.food.controller

import com.gerd.domain.food.dto.AdminUserFoodDTO
import com.gerd.domain.food.service.AdminFoodService
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import com.gerd.global.common.response.PageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminFoodController(
    private val adminFoodService: AdminFoodService,
) : AdminFoodApi {

    override fun getAllUserFoods(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(required = false) isUnknown: Boolean?,
    ): ResponseEntity<ApiResponse<PageResponse<AdminUserFoodDTO>>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(adminFoodService.getAllUserFoods(page, isUnknown), CommonSuccessCode.OK))

    override fun promote(
        @PathVariable foodExternalId: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        adminFoodService.promote(foodExternalId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }
}
