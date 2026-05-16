package com.gerd.global.apiPayload.code

import org.springframework.http.HttpStatus

interface BaseSuccessCode {
    val httpStatus: HttpStatus
    val code: String
    val message: String
}
