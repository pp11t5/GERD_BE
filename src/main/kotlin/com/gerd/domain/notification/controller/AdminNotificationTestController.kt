package com.gerd.domain.notification.controller

import com.gerd.domain.notification.dto.AdminNotificationTestRequestDTO
import com.gerd.domain.notification.service.AdminNotificationTestService
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.CommonSuccessCode
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// prod에는 이 컨트롤러 자체가 로드되지 않는다 — 실제 유저에게 임의 발송되는 것을 원천 차단
@Profile("!prod")
@RestController
class AdminNotificationTestController(
    private val adminNotificationTestService: AdminNotificationTestService,
) : AdminNotificationTestApi {

    override fun sendTest(
        @Valid @RequestBody request: AdminNotificationTestRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>> {
        adminNotificationTestService.send(request.userId, request.type, request.targetId)
        return ResponseEntity
            .status(CommonSuccessCode.OK.httpStatus)
            .body(ApiResponse.onSuccess(Unit, CommonSuccessCode.OK))
    }
}
