package com.gerd.domain.fcm.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.fcm.service.FcmTokenService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(controllers = [FcmController::class])
@AutoConfigureMockMvc(addFilters = false)
class FcmControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean private lateinit var fcmTokenService: FcmTokenService
    @MockitoBean private lateinit var jwtProvider: JwtProvider

    @Nested
    inner class `POST register` {

        @Nested
        inner class 성공 {

            @Test
            @WithCustomUser
            fun `FCM 토큰을 등록한다`() {
                doNothing().whenever(fcmTokenService).register(any(), any())

                mockMvc.post("/api/v1/fcm/tokens") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(
                        mapOf("platform" to "ios", "token" to "fcm-token-abc")
                    )
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            @WithCustomUser
            fun `token이 빈 값이면 COMMON400_1을 반환한다`() {
                mockMvc.post("/api/v1/fcm/tokens") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(
                        mapOf("platform" to "ios", "token" to "")
                    )
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("COMMON400_1") }
                }
            }
        }
    }

    @Nested
    inner class `DELETE delete` {

        @Nested
        inner class 성공 {

            @Test
            @WithCustomUser
            fun `FCM 토큰을 삭제한다`() {
                doNothing().whenever(fcmTokenService).delete(any())

                mockMvc.delete("/api/v1/fcm/tokens").andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            @WithCustomUser
            fun `토큰이 없으면 FCM404_1을 반환한다`() {
                whenever(fcmTokenService.delete(any()))
                    .thenThrow(GeneralException(FcmErrorCode.FCM_TOKEN_NOT_FOUND))

                mockMvc.delete("/api/v1/fcm/tokens").andExpect {
                    status { isNotFound() }
                    jsonPath("$.isSuccess") { value(false) }
                    jsonPath("$.code") { value("FCM404_1") }
                }
            }
        }
    }
}
