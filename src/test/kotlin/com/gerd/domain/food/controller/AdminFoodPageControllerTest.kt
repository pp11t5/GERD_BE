package com.gerd.domain.food.controller

import com.gerd.domain.auth.security.AccessTokenBlacklist
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.dto.AdminUserFoodDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.service.AdminFoodService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.common.response.PageResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [AdminFoodPageController::class])
@AutoConfigureMockMvc(addFilters = false)
class AdminFoodPageControllerTest @Autowired constructor(
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

    @Test
    fun `로그인 페이지를 렌더링한다`() {
        mockMvc.get("/admin/login").andExpect {
            status { isOk() }
            view { name("admin/login") }
        }
    }

    @Test
    @WithMockUser(username = "admin@gerd.com", roles = ["ADMIN"])
    fun `음식 목록 페이지에 유저 음식 이름과 승격 버튼이 렌더링된다`() {
        val dto = AdminUserFoodDTO(externalId = "uuid-1", name = "된장찌개")
        whenever(adminFoodService.getAllUserFoods(any(), anyOrNull())).thenReturn(pageResponse(dto))

        mockMvc.get("/admin/foods") {
            principal = UsernamePasswordAuthenticationToken("admin@gerd.com", null)
        }.andExpect {
            status { isOk() }
            view { name("admin/foods") }
            content { string(org.hamcrest.Matchers.containsString("된장찌개")) }
            content { string(org.hamcrest.Matchers.containsString("/admin/foods/uuid-1/promote")) }
        }
    }

    @Test
    @WithMockUser(username = "admin@gerd.com", roles = ["ADMIN"])
    fun `승격 성공 시 목록으로 리다이렉트한다`() {
        mockMvc.post("/admin/foods/uuid-1/promote") {
            param("page", "2")
            param("isUnknown", "true")
        }.andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/admin/foods?page=2&isUnknown=true")
            flash { attribute("message", "음식을 승격했습니다.") }
        }

        verify(adminFoodService).promote("uuid-1")
    }

    @Test
    @WithMockUser(username = "admin@gerd.com", roles = ["ADMIN"])
    fun `승격 실패 시 오류 메시지와 함께 리다이렉트한다`() {
        doThrow(GeneralException(FoodErrorCode.FOOD_NOT_FOUND)).whenever(adminFoodService).promote("missing")

        mockMvc.post("/admin/foods/missing/promote").andExpect {
            status { is3xxRedirection() }
            flash { attribute("error", FoodErrorCode.FOOD_NOT_FOUND.message) }
        }
    }
}
