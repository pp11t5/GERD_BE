package com.gerd.domain.judgment.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.judgment.dto.JudgmentResponseDTO
import com.gerd.domain.judgment.service.FoodJudgmentQueryService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class JudgmentController(
    private val foodJudgmentQueryService: FoodJudgmentQueryService,
) : JudgmentApi {

    override fun getJudgment(
        @CurrentUser userDetails: CustomUserDetails,
        foodExternalId: String,
    ): ResponseEntity<ApiResponse<JudgmentResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(
                ApiResponse.onSuccess(
                    foodJudgmentQueryService.getJudgment(foodExternalId, userDetails.userId),
                    CommonSuccessCode.OK,
                ),
            )
}
