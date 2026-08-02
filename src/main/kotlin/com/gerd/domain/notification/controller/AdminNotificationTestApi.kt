package com.gerd.domain.notification.controller

import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.notification.dto.AdminNotificationTestRequestDTO
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "관리자 알림 테스트 발송", description = "prod에는 노출되지 않는 알림 테스트 API")
@RequestMapping("/api/v1/admin/notifications")
interface AdminNotificationTestApi {

    @Operation(
        summary = "테스트 알림 발송",
        description = """
            지정한 유저에게 등록된 FCM 토큰으로 실제 알림과 동일한 문구를 즉시 발송합니다.
            post_meal/post_meal_delayed_single은 targetId(식사 기록 ID)를 함께 보낼 수 있습니다.
            prod 환경에서는 이 API 자체가 노출되지 않습니다.
        """,
    )
    @ApiErrorExample(FcmErrorCode::class, "FCM_TOKEN_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "발송 성공"))
    @PostMapping("/test")
    fun sendTest(
        @Valid @RequestBody request: AdminNotificationTestRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>>
}
