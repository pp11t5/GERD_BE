package com.gerd.domain.onboarding.controller

import com.gerd.domain.auth.security.CustomUserDetails
import com.gerd.domain.onboarding.dto.OnboardingRequestDTO
import com.gerd.domain.onboarding.dto.OnboardingStatusResponseDTO
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Onboarding", description = "온보딩 API")
@RequestMapping("/api/v1/onboarding")
interface OnboardingApi {

    @Operation(
        summary = "온보딩 완료 여부 조회",
        description = "로그인 후 온보딩/홈 라우팅 판단용. user_profiles row 존재 여부를 반환합니다.",
    )
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/status")
    fun getStatus(
        @CurrentUser userDetails: CustomUserDetails,
    ): ResponseEntity<ApiResponse<OnboardingStatusResponseDTO>>

    @Operation(
        summary = "온보딩 일괄 제출",
        description = """
            온보딩 4단계(증상/트리거/알레르기/복용약)를 한 번에 제출합니다(단일 트랜잭션).
            - symptoms / triggers / allergens 는 한글 라벨이 아니라 영어 code(enum)로 전달합니다. 허용값은 요청 스키마의 enum 참고.
            - 요청 예시: { "symptoms": ["heartburn_reflux"], "triggers": ["caffeine","spicy"], "allergens": ["milk"], "medications": ["PPI"], "customTriggerText": "오렌지주스" }
            - 이미 온보딩을 완료한 경우 409로 거부합니다.
        """,
    )
    @ApiErrorExample(
        OnboardingErrorCode::class,
        "ALREADY_ONBOARDED",
        "INVALID_TRIGGER",
        "INVALID_ALLERGEN",
    )
    @ApiResponses(SwaggerResponse(responseCode = "201", description = "온보딩 제출 성공"))
    @PostMapping
    fun submit(
        @CurrentUser userDetails: CustomUserDetails,
        @RequestBody request: OnboardingRequestDTO,
    ): ResponseEntity<ApiResponse<Unit>>
}
