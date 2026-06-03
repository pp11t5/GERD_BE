package com.gerd.domain.food.controller

import com.gerd.domain.food.dto.AddRecentRequestDTO
import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.dto.RecentFoodDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.service.FoodSearchService
import com.gerd.domain.food.service.RecentFoodService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

@WebMvcTest(controllers = [FoodController::class])
@AutoConfigureMockMvc(addFilters = false)
class FoodControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    private lateinit var foodSearchService: FoodSearchService

    @MockitoBean
    private lateinit var recentFoodService: RecentFoodService

    // 보안 필터 체인(JwtAuthenticationFilter)이 컨텍스트에서 요구하는 빈 — addFilters=false라 동작은 안 하지만 빈은 필요
    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    private fun recentFood() = RecentFoodDTO(
        recentId = 1024,
        foodExternalId = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f",
        name = "된장찌개",
        categories = listOf("soup_stew"),
        searchedAt = LocalDateTime.of(2026, 6, 3, 8, 12, 0),
    )

    @Nested
    inner class `GET search` {

        @Test
        @WithCustomUser
        fun `검색에 성공하면 음식 목록을 반환한다`() {
            whenever(foodSearchService.search(any(), anyOrNull(), any()))
                .thenReturn(listOf(FoodSummaryDTO("ext-1", "된장찌개", listOf("soup_stew"))))

            mockMvc.get("/api/v1/foods/search") {
                param("q", "된장")
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.result[0].name") { value("된장찌개") }
                jsonPath("$.result[0].categories[0]") { value("soup_stew") }
            }
        }

        @Test
        @WithCustomUser
        fun `검색어가 올바르지 않으면 FOOD400_1`() {
            whenever(foodSearchService.search(any(), anyOrNull(), any()))
                .thenThrow(GeneralException(FoodErrorCode.INVALID_SEARCH_QUERY))

            mockMvc.get("/api/v1/foods/search") {
                param("q", " ")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("FOOD400_1") }
            }
        }
    }

    @Nested
    inner class `GET recent` {

        @Test
        @WithCustomUser
        fun `최근 본 음식 목록을 반환한다`() {
            whenever(recentFoodService.getRecent(anyOrNull(), any())).thenReturn(listOf(recentFood()))

            mockMvc.get("/api/v1/foods/recent").andExpect {
                status { isOk() }
                jsonPath("$.result[0].recentId") { value(1024) }
                jsonPath("$.result[0].searchedAt") { value("2026-06-03 08:12:00") }
            }
        }
    }

    @Nested
    inner class `POST recent` {

        @Test
        @WithCustomUser
        fun `추가에 성공하면 RecentFood를 반환한다`() {
            whenever(recentFoodService.addRecent(any(), any())).thenReturn(recentFood())
            val body = AddRecentRequestDTO(foodExternalId = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")

            mockMvc.post("/api/v1/foods/recent") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.recentId") { value(1024) }
            }
        }

        @Test
        @WithCustomUser
        fun `음식을 찾을 수 없으면 FOOD404_1`() {
            whenever(recentFoodService.addRecent(any(), any()))
                .thenThrow(GeneralException(FoodErrorCode.FOOD_NOT_FOUND))
            val body = AddRecentRequestDTO(foodExternalId = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")

            mockMvc.post("/api/v1/foods/recent") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("FOOD404_1") }
            }
        }
    }

    @Nested
    inner class `DELETE recent` {

        @Test
        @WithCustomUser
        fun `단건 삭제에 성공한다`() {
            mockMvc.delete("/api/v1/foods/recent/1024").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
            }
        }

        @Test
        @WithCustomUser
        fun `없는 항목이면 FOOD404_2`() {
            whenever(recentFoodService.deleteRecent(any(), any()))
                .thenThrow(GeneralException(FoodErrorCode.RECENT_NOT_FOUND))

            mockMvc.delete("/api/v1/foods/recent/999").andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("FOOD404_2") }
            }
        }

        @Test
        @WithCustomUser
        fun `전체 삭제에 성공한다`() {
            mockMvc.delete("/api/v1/foods/recent").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
            }
        }
    }
}
