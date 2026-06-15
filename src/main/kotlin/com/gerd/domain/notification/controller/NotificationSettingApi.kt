package com.gerd.domain.notification.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.notification.dto.DailyNotificationTimeUpdateRequestDTO
import com.gerd.domain.notification.dto.NotificationSettingResponseDTO
import com.gerd.domain.notification.dto.NotificationSettingToggleRequestDTO
import com.gerd.domain.notification.exception.NotificationErrorCode
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "알림 설정", description = "알림 설정 조회 및 수정 API")
@RequestMapping("/api/v1/notifications/settings")
interface NotificationSettingApi {

    @Operation(summary = "알림 설정 조회"
        , description = """
            현재 사용자의 알림 설정을 조회합니다.
            온보딩 시 약관동의 기준으로 기본값이 설정됩니다.
            - post_meal: 식사 후 기록 알림
            - daily_record: 매일 기록 알림
            - weekly_report: 주간 리포트 알림
            - daily_notification_time: 매일 밤 기록 알림 시간대 (기본값: 오전 8시)
        """
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @ApiErrorExample(NotificationErrorCode::class, "NOTIFICATION_SETTING_NOT_FOUND")
    @GetMapping
    fun getNotificationSetting(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<NotificationSettingResponseDTO>>

    @Operation(
        summary = "알림 설정 수정 (항목 토글 방식)",
        description = """
            type에 따른 알림 설정을 수정합니다. 알림은 켜져 있으면 끄고, 꺼져 있으면 켜는 토글 방식으로 동작합니다.
            type: post_meal(식사 후 기록 알림) | daily_record(매일 기록 알림) | weekly_report(주간 리포트 알림)
            """,
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "토글 성공"))
    @ApiErrorExample(NotificationErrorCode::class, "NOTIFICATION_SETTING_NOT_FOUND")
    @PatchMapping("/toggle")
    fun toggleNotificationSetting(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody request: NotificationSettingToggleRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "매일 밤 기록 알림 시간대 수정",
        description = """
            매일 밤 기록 알림 시간대를 수정합니다. 기본은 오전 8시입니다.
            오전 8시, 오후 8시, 밤 9시, 밤 10시 중 하나로 변경합니다.
            time: morning_8 | evening_8 | night_9 | night_10
        """,
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "수정 성공"))
    @ApiErrorExample(NotificationErrorCode::class, "NOTIFICATION_SETTING_NOT_FOUND")
    @PatchMapping("/daily-time")
    fun updateDailyNotificationTime(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody request: DailyNotificationTimeUpdateRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>>
}
