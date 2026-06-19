package com.gerd.domain.meal.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class MealErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    INVALID_DATE_TIME(HttpStatus.BAD_REQUEST, "MEAL400_2", "날짜/시간 형식이 올바르지 않습니다."),
    MEAL_FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "MEAL404_1", "식사 음식 기록을 찾을 수 없습니다."),
    MEAL_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "MEAL404_2", "끼니를 찾을 수 없습니다."),
}
