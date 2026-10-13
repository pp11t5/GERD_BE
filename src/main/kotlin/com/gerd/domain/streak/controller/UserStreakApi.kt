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

@Tag(name = "UserStreak", description = "사용자 편안한 증상 기록 스트릭 API")
@RequestMapping("/api/v1/users/me/streak")
interface UserStreakApi {

    @Operation(
        summary = "내 편안한 증상 기록 스트릭 조회",
        description = """진입점 - 홈 화면, 현재 사용자의 편안한 증상 기록 연속일과 홈 캐릭터 상태를 조회합니다. 오늘 또는 어제 기준 연속 기록이 없으면 streak은 0입니다.

characterStatus는 아래 기준으로 결정됩니다.
- greeting: 증상 기록과 식사 기록이 모두 한 번도 없는 경우
- grateful: 편안 스트릭이 7일 이상인 경우
- satisfied: 편안 스트릭이 1~6일이거나, 스트릭이 0이지만 최근 증상이 불편·심함이 아닌 경우
- terrible: 편안 스트릭이 0이고 가장 최근 증상 기록이 불편 또는 심함인 경우""",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping
    fun getMyStreak(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<UserStreakResponseDTO>>
}
