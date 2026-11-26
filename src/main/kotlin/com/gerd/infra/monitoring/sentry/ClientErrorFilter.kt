package com.gerd.infra.monitoring.sentry

import com.gerd.global.apiPayload.GeneralException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.ErrorResponse
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

object ClientErrorFilter {

    fun isClientError(throwable: Throwable?): Boolean = when (throwable) {
        null -> false
        is GeneralException -> isDroppable(throwable.errorCode.httpStatus)
        is ErrorResponse -> isDroppable(throwable.statusCode)
        else -> isRequestError(throwable)
    }

    private fun isRequestError(throwable: Throwable): Boolean =
        throwable is ConstraintViolationException ||
            throwable is MethodArgumentTypeMismatchException ||
            throwable is HttpMessageNotReadableException ||
            throwable is BindException

    private fun isDroppable(status: HttpStatusCode): Boolean =
        status.is4xxClientError && status.value() != HttpStatus.REQUEST_TIMEOUT.value()
}
