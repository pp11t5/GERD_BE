package com.gerd.domain.food.controller

import com.gerd.domain.auth.security.AccessTokenBlacklist
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.dto.AdminUserFoodDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.service.AdminFoodService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.common.response.PageResponse
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [AdminFoodController::class])
@AutoConfigureMockMvc(addFilters = false)
class AdminFoodControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var adminFoodService: AdminFoodService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider


    @MockitoBean
    private lateinit var accessTokenBlacklist: AccessTokenBlacklist

    private fun pageResponse(vararg items: AdminUserFoodDTO) = PageResponse(
        content = items.toList(),
        page = 0,
        size = 100,
        totalElements = items.size.toLong(),
        totalPages = 1,
        hasNext = false,
        isFirst = true,
        isLast = true,
    )

    @Nested
    inner class `GET admin foods` {

        @Test
        @WithCustomUser(role = "ADMIN")
        fun `전체 유저 음식을 조회한다`() {
            val dto = AdminUserFoodDTO(externalId = "uuid-1", name = "된장찌개")
            whenever(adminFoodService.getAllUserFoods(any(), anyOrNull())).thenReturn(pageResponse(dto))

            mockMvc.get("/api/v1/admin/foods").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.result.content[0].name") { value("된장찌개") }
                jsonPath("$.result.content[0].externalId") { value("uuid-1") }
                jsonPath("$.result.totalElements") { value(1) }
            }
        }

        @Test
        @WithCustomUser(role = "ADMIN")
        fun `isUnknown 파라미터를 서비스에 전달한다`() {
            whenever(adminFoodService.getAllUserFoods(any(), anyOrNull())).thenReturn(pageResponse())

            mockMvc.get("/api/v1/admin/foods") {
                param("isUnknown", "true")
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.content") { isArray() }
            }
        }

        @Test
        @WithCustomUser(role = "ADMIN")
        fun `결과가 없으면 빈 목록을 반환한다`() {
            whenever(adminFoodService.getAllUserFoods(any(), anyOrNull())).thenReturn(pageResponse())

            mockMvc.get("/api/v1/admin/foods").andExpect {
                status { isOk() }
                jsonPath("$.result.content.length()") { value(0) }
                jsonPath("$.result.totalElements") { value(0) }
            }
        }

        @Test
        @WithCustomUser(role = "ADMIN")
        fun `음수 페이지 번호이면 400을 반환한다`() {
            mockMvc.get("/api/v1/admin/foods") {
                param("page", "-1")
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    inner class `POST promote` {

        @Test
        @WithCustomUser(role = "ADMIN")
        fun `승격에 성공하면 200을 반환한다`() {
            mockMvc.post("/api/v1/admin/foods/valid-uuid/promote").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
            }
        }

        @Test
        @WithCustomUser(role = "ADMIN")
        fun `음식을 찾을 수 없으면 FOOD404_1`() {
            whenever(adminFoodService.promote(any()))
                .thenThrow(GeneralException(FoodErrorCode.FOOD_NOT_FOUND))

            mockMvc.post("/api/v1/admin/foods/nonexistent-uuid/promote").andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("FOOD404_1") }
            }
        }
    }
}
