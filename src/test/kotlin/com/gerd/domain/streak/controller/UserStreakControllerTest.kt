package com.gerd.domain.streak.controller

import com.gerd.domain.auth.security.AccessTokenBlacklist
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.streak.dto.StreakCharacterStatus
import com.gerd.domain.streak.dto.UserStreakResponseDTO
import com.gerd.domain.streak.service.UserStreakService
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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


    @MockitoBean
    private lateinit var accessTokenBlacklist: AccessTokenBlacklist

    @Nested
    inner class `GET 내 스트릭 조회` {

        @Test
        @WithCustomUser(userId = 1L)
        fun `오늘 기준 스트릭을 반환한다`() {
            whenever(userStreakService.getStreak(1L)).thenReturn(
                UserStreakResponseDTO(streak = 3, characterStatus = StreakCharacterStatus.SATISFIED),
            )

            mockMvc.get("/api/v1/users/me/streak").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
                jsonPath("$.message") { value("요청에 성공하였습니다.") }
                jsonPath("$.result.streak") { value(3) }
                jsonPath("$.result.characterStatus") { value("satisfied") }
            }

            verify(userStreakService).getStreak(1L)
        }

        @ParameterizedTest
        @CsvSource(
            "GREETING, greeting",
            "TERRIBLE, terrible",
            "SATISFIED, satisfied",
            "GRATEFUL, grateful",
        )
        @WithCustomUser(userId = 1L)
        fun `캐릭터 상태를 소문자 문자열로 직렬화한다`(
            status: StreakCharacterStatus,
            expected: String,
        ) {
            whenever(userStreakService.getStreak(1L)).thenReturn(
                UserStreakResponseDTO(streak = 0, characterStatus = status),
            )

            mockMvc.get("/api/v1/users/me/streak").andExpect {
                status { isOk() }
                jsonPath("$.result.characterStatus") { value(expected) }
            }
        }
    }
}
