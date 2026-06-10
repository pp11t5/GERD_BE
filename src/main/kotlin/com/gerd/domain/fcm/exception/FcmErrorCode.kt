package com.gerd.domain.fcm.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class FcmErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM404_1", "FCM 토큰을 찾을 수 없습니다."),
    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FCM500_1", "푸시 알림 발송에 실패했습니다."),
}
