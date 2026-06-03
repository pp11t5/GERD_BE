package com.gerd.domain.food.exception

import com.gerd.global.apiPayload.code.BaseErrorCode
import org.springframework.http.HttpStatus

enum class FoodErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {

    INVALID_SEARCH_QUERY(HttpStatus.BAD_REQUEST, "FOOD400_1", "검색어가 올바르지 않습니다."),
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "FOOD404_1", "음식을 찾을 수 없습니다."),
    RECENT_NOT_FOUND(HttpStatus.NOT_FOUND, "FOOD404_2", "최근 검색 항목을 찾을 수 없습니다."),
}
