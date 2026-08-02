package com.gerd.domain.notification.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.fcm.exception.FcmErrorCode
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.service.AdminNotificationTestService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [AdminNotificationTestController::class])
@AutoConfigureMockMvc(addFilters = false)
class AdminNotificationTestControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var adminNotificationTestService: AdminNotificationTestService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @Test
    @WithCustomUser(role = "ADMIN")
    fun `발송에 성공하면 200을 반환한다`() {
        mockMvc.post("/api/v1/admin/notifications/test") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId":1,"type":"post_meal","targetId":"100"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.isSuccess") { value(true) }
        }

        verify(adminNotificationTestService).send(1L, NotificationType.POST_MEAL, "100")
    }

    @Test
    @WithCustomUser(role = "ADMIN")
    fun `targetId 없이도 요청할 수 있다`() {
        mockMvc.post("/api/v1/admin/notifications/test") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId":1,"type":"daily_record"}"""
        }.andExpect {
            status { isOk() }
        }

        verify(adminNotificationTestService).send(eq(1L), eq(NotificationType.DAILY_RECORD), isNull())
    }

    @Test
    @WithCustomUser(role = "ADMIN")
    fun `대상 유저의 FCM 토큰이 없으면 FCM404_1`() {
        doThrow(GeneralException(FcmErrorCode.FCM_TOKEN_NOT_FOUND))
            .whenever(adminNotificationTestService).send(any(), any(), anyOrNull())

        mockMvc.post("/api/v1/admin/notifications/test") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId":999,"type":"post_meal"}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("FCM404_1") }
        }
    }
}
