package com.gerd.infra.monitoring.sentry

import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.apiPayload.code.CommonErrorCode
import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class ClientErrorFilterTest {

    @Nested
    inner class `isClientError` {

        @Test
        fun `4xx 도메인 예외는 클라이언트 오류로 본다`() {
            assertThat(ClientErrorFilter.isClientError(GeneralException(CommonErrorCode.INVALID_REQUEST))).isTrue()
        }

        @Test
        fun `5xx 도메인 예외는 클라이언트 오류로 보지 않는다`() {
            assertThat(ClientErrorFilter.isClientError(GeneralException(CommonErrorCode.INTERNAL_SERVER_ERROR))).isFalse()
        }

        @Test
        fun `408 타임아웃은 서버 쪽 신호라 걸러내지 않는다`() {
            assertThat(ClientErrorFilter.isClientError(GeneralException(CommonErrorCode.REQUEST_TIMEOUT))).isFalse()
        }

        @Test
        fun `4xx 상태를 가진 스프링 예외는 클라이언트 오류로 본다`() {
            assertThat(ClientErrorFilter.isClientError(ResponseStatusException(HttpStatus.NOT_FOUND))).isTrue()
            assertThat(ClientErrorFilter.isClientError(ResponseStatusException(HttpStatus.BAD_GATEWAY))).isFalse()
        }

        @Test
        fun `요청 검증 예외는 클라이언트 오류로 본다`() {
            assertThat(ClientErrorFilter.isClientError(ConstraintViolationException(emptySet()))).isTrue()
        }

        @Test
        fun `그 밖의 예외와 null은 클라이언트 오류로 보지 않는다`() {
            assertThat(ClientErrorFilter.isClientError(IllegalStateException("boom"))).isFalse()
            assertThat(ClientErrorFilter.isClientError(null)).isFalse()
        }
    }
}
