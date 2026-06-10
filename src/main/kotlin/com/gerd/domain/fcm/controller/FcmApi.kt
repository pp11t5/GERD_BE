package com.gerd.domain.fcm.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.fcm.dto.FcmTokenRegisterRequestDTO
import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "FCM", description = "FCM 푸시 토큰 API")
@RequestMapping("/api/v1/fcm/tokens")
interface FcmApi {

    @Operation(
        summary = "FCM 토큰 등록/갱신",
        description = """
            로그인 후 FCM SDK에서 발급받은 토큰을 등록합니다.
            - 이미 등록된 경우 token과 platform을 갱신합니다(upsert).
            - platform: ios | android
        """,
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "등록/갱신 성공"))
    @PostMapping
    fun register(
        @CurrentUser userDetails: CustomUserDetails,
        @Valid @RequestBody request: FcmTokenRegisterRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "FCM 토큰 삭제",
        description = """
            로그아웃 시 호출합니다. 등록된 FCM 토큰을 삭제합니다.
        """,
    )
    @ApiErrorExample(FcmErrorCode::class, "FCM_TOKEN_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "삭제 성공"))
    @DeleteMapping
    fun delete(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Unit>>
}
