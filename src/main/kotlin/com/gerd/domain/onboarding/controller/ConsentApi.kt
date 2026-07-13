package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.dto.TermResponseDTO
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Consent", description = "약관동의 API")
@RequestMapping("/api/v1/consent")
interface ConsentApi {

    @Operation(summary = "약관 목록 조회", description = "code별 최신 버전 약관 목록을 반환합니다. 인증 불필요.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/terms")
    fun getTerms(): ResponseEntity<ApiResponse<List<TermResponseDTO>>>

    @Operation(
        summary = "마케팅 동의 토글",
        description = "마케팅 수신 동의를 on/off 전환합니다. 변경 후 동의 상태(agreed)를 반환합니다.",
    )
    @ApiErrorExample(OnboardingErrorCode::class, "MARKETING_CONSENT_NOT_FOUND")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "토글 성공"))
    @PatchMapping("/marketing/toggle")
    fun toggleMarketing(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Boolean>>

    @Operation(
        summary = "약관동의 제출",
        description = """
            로그인 직후, 온보딩 진입 전 약관동의를 제출합니다.
            - required=true 약관 중 하나라도 agreed=false면 거부합니다.
            - marketing은 선택이며 false도 그대로 저장합니다.
            - 재호출 시 해당 버전의 동의 상태·시점이 갱신됩니다.
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
