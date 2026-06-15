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
    ): ResponseEntity<ApiResponse<JudgmentResponseDTO>> {
        val (response, isCached) = foodJudgmentQueryService.getJudgment(foodExternalId, userDetails.userId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .header("X-Cache", if (isCached) "HIT" else "MISS")
            .body(ApiResponse.onSuccess(response, CommonSuccessCode.OK))
    }
}
