package com.gerd.domain.meal.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class MealErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "MEAL400_1", "메모는 200자 이내로 작성해주세요."),
    INVALID_DATE_TIME(HttpStatus.BAD_REQUEST, "MEAL400_2", "날짜/시간 형식이 올바르지 않습니다."),
    MEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "MEAL404_1", "식사 기록을 찾을 수 없습니다."),
    MEAL_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "MEAL404_2", "끼니를 찾을 수 없습니다."),
}
