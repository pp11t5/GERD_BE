package com.gerd.domain.auth.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_1", "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_5", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_2", "만료된 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401_3", "인증이 필요합니다."),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH400_1", "이메일 제공에 동의가 필요합니다."),
    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH400_3", "닉네임 제공에 동의가 필요합니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400_2", "지원하지 않는 소셜 로그인 제공자입니다."),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "AUTH403_2", "비활성화된 계정입니다. 복구 절차를 진행해주세요."),
    ACCOUNT_RECOVERABLE(HttpStatus.FORBIDDEN, "AUTH403_5", "탈퇴 처리 중인 계정입니다. 복구가 가능합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404_1", "사용자를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403_1", "접근 권한이 없습니다."),
    KAKAO_UNLINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_1", "카카오 연결해제에 실패했습니다."),
}
