package com.gerd.domain.auth.client

import com.gerd.domain.auth.exception.AuthErrorCode
import com.gerd.domain.auth.util.AppleLoginUtil
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.config.properties.AppleProperties
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.web.client.RestClient

class AppleApiClientTest {

    private val appleProperties = AppleProperties(
        clientId = "com.canieatthis.canIEatThis.dev",
        tokenUrl = "https://apple.test/auth/token",
    )

    @Nested
    inner class `issueToken` {

        @Test
        fun `Apple이 invalid_grant를 반환하면 AUTH400_3을 던진다`() {
            val fixture = createClient()
            fixture.server.expect(requestTo(appleProperties.tokenUrl))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(
                    withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""{"error":"invalid_grant"}"""),
                )

            assertThatThrownBy { fixture.client.issueToken("authorization-code", "client-secret") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.APPLE_INVALID_AUTHORIZATION_CODE)
        }

        @Test
        fun `Apple 서버 오류는 기존 AUTH502_1을 유지한다`() {
            val fixture = createClient()
            fixture.server.expect(requestTo(appleProperties.tokenUrl))
                .andRespond(withServerError())

            assertThatThrownBy { fixture.client.issueToken("authorization-code", "client-secret") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.APPLE_TOKEN_REQUEST_FAILED)
        }
    }

    private fun createClient(): AppleClientFixture {
        val builder = RestClient.builder().requestFactory(JdkClientHttpRequestFactory())
        val server = MockRestServiceServer.bindTo(builder).build()
        return AppleClientFixture(
            client = AppleApiClient(appleProperties, builder.build(), mock<AppleLoginUtil>()),
            server = server,
        )
    }

    private data class AppleClientFixture(
        val client: AppleApiClient,
        val server: MockRestServiceServer,
    )
}
