package com.gerd.global.apiPayload

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.gerd.global.apiPayload.code.BaseErrorCode
import com.gerd.global.apiPayload.code.BaseSuccessCode
import com.gerd.global.apiPayload.code.CommonSuccessCode
import org.slf4j.MDC

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    @get:JsonProperty("isSuccess")
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: T? = null,
    val traceId: String? = null,  // 실패 응답에만 포함
) {
    companion object {

        fun <T> onSuccess(
            result: T,
            successCode: BaseSuccessCode = CommonSuccessCode.OK,
        ): ApiResponse<T> =
            ApiResponse(
                isSuccess = true,
                code = successCode.code,
                message = successCode.message,
                result = result,
            )

        fun <T> onFailure(errorCode: BaseErrorCode, detail: T? = null): ApiResponse<T> =
            ApiResponse(
                isSuccess = false,
                code = errorCode.code,
                message = errorCode.message,
                result = detail,
                traceId = MDC.get("traceId"),
            )
    }
}
