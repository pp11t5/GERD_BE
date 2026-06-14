package com.gerd.domain.notification.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class NotificationErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    NOTIFICATION_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI404_1", "알림 설정을 찾을 수 없습니다."),
}
