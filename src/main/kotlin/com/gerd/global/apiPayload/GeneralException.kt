package com.gerd.global.apiPayload

import com.gerd.global.apiPayload.code.BaseErrorCode

class GeneralException(
    val errorCode: BaseErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)
