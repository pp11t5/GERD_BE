package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.global.annotation.ApiErrorExample
import com.gerd.global.annotation.CurrentUser
import com.gerd.global.apiPayload.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Consent", description = "약관동의 API")
@RequestMapping("/api/v1/consent")
interface ConsentApi {

    @Operation(
        summary = "약관동의 제출",
        description = """
            로그인 직후, 온보딩 진입 전 약관동의를 제출합니다.
            - tos / privacy / health_sensitive 는 필수이며 셋 중 하나라도 false면 거부합니다.
            - marketing 은 선택이며 false도 그대로 저장합니다.
            - 재호출 시 type별 현재 동의 상태와 시점이 갱신됩니다(마케팅 철회 등).
        """,
    )
    @ApiErrorExample(OnboardingErrorCode::class, "REQUIRED_CONSENT_NOT_AGREED")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "동의 저장 성공"))
    @PostMapping
    fun submitConsent(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody request: ConsentRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>>
}
