package com.gerd.domain.auth.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.gerd.global.apiPayload.ApiResponse
import com.gerd.global.apiPayload.code.BaseErrorCode
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import java.io.IOException

object SecurityErrorResponseWriter {
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Throws(IOException::class)
    fun write(
        response: HttpServletResponse,
        errorCode: BaseErrorCode,
    ) {
        if (response.isCommitted) {
            return
        }

        response.status = errorCode.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()

        val body = ApiResponse.onFailure<Any?>(errorCode, null)
        objectMapper.writeValue(response.outputStream, body)
    }
}
