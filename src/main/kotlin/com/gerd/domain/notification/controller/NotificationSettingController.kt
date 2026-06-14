package com.gerd.domain.notification.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.notification.dto.DailyNotificationTimeUpdateRequestDTO
import com.gerd.domain.notification.dto.NotificationSettingResponseDTO
import com.gerd.domain.notification.dto.NotificationSettingToggleRequestDTO
import com.gerd.domain.notification.service.NotificationSettingService
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class NotificationSettingController(
    private val notificationSettingService: NotificationSettingService,
) : NotificationSettingApi {

    override fun getNotificationSetting(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<NotificationSettingResponseDTO>> =
        ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(notificationSettingService.getNotificationSetting(userDetails.userId)))

    override fun toggleNotificationSetting(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: NotificationSettingToggleRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>> {
        notificationSettingService.toggleNotificationSetting(userDetails.userId, request.type)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit))
    }

    override fun updateDailyNotificationTime(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: DailyNotificationTimeUpdateRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>> {
        notificationSettingService.updateDailyNotificationTime(userDetails.userId, request.time)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit))
    }
}
