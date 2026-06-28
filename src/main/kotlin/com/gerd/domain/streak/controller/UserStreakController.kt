package com.gerd.domain.streak.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.streak.dto.UserStreakResponseDTO
import com.gerd.domain.streak.service.UserStreakService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UserStreakController(
    private val userStreakService: UserStreakService,
) : UserStreakApi {

    override fun getMyStreak(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<UserStreakResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(userStreakService.getStreak(userDetails.userId), CommonSuccessCode.OK))
}
