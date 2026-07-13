package com.gerd.domain.onboarding.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class OnboardingErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    REQUIRED_CONSENT_NOT_AGREED(HttpStatus.BAD_REQUEST, "ONBOARD400_1", "필수 약관에 모두 동의해야 합니다."),
    INVALID_TRIGGER(HttpStatus.BAD_REQUEST, "ONBOARD400_2", "존재하지 않는 트리거 음식입니다."),
    INVALID_ALLERGEN(HttpStatus.BAD_REQUEST, "ONBOARD400_3", "존재하지 않는 알레르기 항목입니다."),
    ALREADY_ONBOARDED(HttpStatus.CONFLICT, "ONBOARD409_1", "이미 온보딩을 완료한 사용자입니다."),
    MARKETING_CONSENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ONBOARD404_1", "마케팅 약관 동의 내역이 없습니다."),
}
