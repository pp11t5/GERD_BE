package com.gerd.domain.streak.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.streak.dto.UserStreakResponseDTO
import com.gerd.domain.streak.service.UserStreakService
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [UserStreakController::class])
@AutoConfigureMockMvc(addFilters = false)
class UserStreakControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var userStreakService: UserStreakService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @Nested
    inner class `GET 내 스트릭 조회` {

        @Test
        @WithCustomUser(userId = 1L)
        fun `오늘 기준 스트릭을 반환한다`() {
            whenever(userStreakService.getStreak(1L)).thenReturn(UserStreakResponseDTO(streak = 3))

            mockMvc.get("/api/v1/users/me/streak").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.result.streak") { value(3) }
            }

            verify(userStreakService).getStreak(1L)
        }
    }
}
