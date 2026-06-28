package com.gerd.domain.streak.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.streak.dto.UserStreakResponseDTO
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "UserStreak", description = "사용자 식사 기록 스트릭 API")
@RequestMapping("/api/v1/users/me/streak")
interface UserStreakApi {

    @Operation(
        summary = "내 식사 기록 스트릭 조회",
        description = "현재 사용자의 식사 기록 연속 작성일을 조회합니다. 식사 기록이 없거나 오늘 기준 스트릭이 끊겼으면 0을 반환합니다.",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping
    fun getMyStreak(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<UserStreakResponseDTO>>
}
