package com.gerd.domain.food.controller

import com.gerd.domain.food.dto.FoodSearchResultDTO
import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.dto.RecentFoodDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.service.FoodCategoryReader
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime

@WebMvcTest(controllers = [FoodController::class])
@AutoConfigureMockMvc(addFilters = false)
class FoodControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var foodSearchService: FoodSearchService

    @MockitoBean
    private lateinit var recentFoodService: RecentFoodService

    @MockitoBean
    private lateinit var foodCategoryReader: FoodCategoryReader

    // 보안 필터 체인(JwtAuthenticationFilter)이 컨텍스트에서 요구하는 빈 — addFilters=false라 동작은 안 하지만 빈은 필요
    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    private fun recentFood() = RecentFoodDTO(
        id = 1L,
        query = "된장찌개",
        searchedAt = LocalDateTime.of(2026, 6, 3, 8, 12, 0),
    )

    @Nested
    inner class `GET search` {

        @Test
        @WithCustomUser
        fun `검색에 성공하면 음식 목록과 hasExactMatch를 반환한다`() {
            whenever(foodSearchService.search(any(), anyOrNull(), any()))
                .thenReturn(FoodSearchResultDTO(
                    foods = listOf(FoodSummaryDTO("ext-1", "된장찌개", "soup_stew")),
                    hasExactMatch = true,
                ))

            mockMvc.get("/api/v1/foods/search") {
                param("q", "된장찌개")
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.result.foods[0].name") { value("된장찌개") }
                jsonPath("$.result.foods[0].category") { value("soup_stew") }
                jsonPath("$.result.hasExactMatch") { value(true) }
            }
        }

        @Test
        @WithCustomUser
        fun `완전 일치가 없으면 hasExactMatch가 false다`() {
            whenever(foodSearchService.search(any(), anyOrNull(), any()))
                .thenReturn(FoodSearchResultDTO(foods = emptyList(), hasExactMatch = false))

            mockMvc.get("/api/v1/foods/search") {
                param("q", "신메뉴")
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.hasExactMatch") { value(false) }
                jsonPath("$.result.foods.length()") { value(0) }
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
        fun `최근 검색어 목록을 반환한다`() {
            whenever(recentFoodService.getRecent(anyOrNull(), any())).thenReturn(listOf(recentFood()))

            mockMvc.get("/api/v1/foods/recent").andExpect {
                status { isOk() }
                jsonPath("$.result[0].query") { value("된장찌개") }
                jsonPath("$.result[0].searchedAt") { value("2026-06-03 08:12:00") }
            }
        }
    }

    @Nested
    inner class `GET top-searched` {

        @Test
        @WithCustomUser
        fun `가장 많이 검색된 상위 음식 목록을 반환한다`() {
            whenever(recentFoodService.getTopSearched()).thenReturn(
                listOf(
                    FoodSummaryDTO("ext-1", "된장찌개", "soup_stew"),
                    FoodSummaryDTO("ext-2", "비빔밥", "rice"),
                ),
            )

            mockMvc.get("/api/v1/foods/top-searched").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.result[0].name") { value("된장찌개") }
                jsonPath("$.result[0].category") { value("soup_stew") }
                jsonPath("$.result[1].name") { value("비빔밥") }
            }
        }

        @Test
        @WithCustomUser
        fun `검색 기록이 없으면 빈 배열을 반환한다`() {
            whenever(recentFoodService.getTopSearched()).thenReturn(emptyList())

            mockMvc.get("/api/v1/foods/top-searched").andExpect {
                status { isOk() }
                jsonPath("$.result.length()") { value(0) }
            }
        }
    }

    @Nested
    inner class `DELETE recent` {

        @Test
        @WithCustomUser
        fun `단건 삭제에 성공한다`() {
            mockMvc.delete("/api/v1/foods/recent/1").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
            }
        }

        @Test
        @WithCustomUser
        fun `없는 항목이면 FOOD404_2`() {
            whenever(recentFoodService.deleteRecent(any(), any()))
                .thenThrow(GeneralException(FoodErrorCode.RECENT_NOT_FOUND))

            mockMvc.delete("/api/v1/foods/recent/1").andExpect {
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
