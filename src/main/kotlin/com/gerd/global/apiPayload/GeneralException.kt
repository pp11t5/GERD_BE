package com.gerd.global.apiPayload

import com.gerd.global.apiPayload.code.BaseErrorCode

class GeneralException(
    val errorCode: BaseErrorCode,
) : RuntimeException(errorCode.message)
