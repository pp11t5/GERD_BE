package com.gerd.domain.auth.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_1", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_2", "만료된 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401_3", "인증이 필요합니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH401_4", "비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404_1", "사용자를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403_1", "접근 권한이 없습니다."),
}
