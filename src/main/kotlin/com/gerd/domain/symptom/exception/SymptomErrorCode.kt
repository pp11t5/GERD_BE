package com.gerd.domain.symptom.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class SymptomErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_DATE_TIME(HttpStatus.BAD_REQUEST, "SYMPTOM400_1", "날짜/시간 형식이 올바르지 않습니다."),
    SYMPTOM_NOT_FOUND(HttpStatus.NOT_FOUND, "SYMPTOM404_1", "증상 기록을 찾을 수 없습니다."),
}
