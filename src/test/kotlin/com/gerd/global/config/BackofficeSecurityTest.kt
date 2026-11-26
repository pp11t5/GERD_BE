package com.gerd.global.config

import com.gerd.domain.auth.filter.JwtAuthenticationFilter
import com.gerd.domain.auth.filter.JwtExceptionFilter
import com.gerd.domain.auth.security.AccessTokenBlacklist
import com.gerd.domain.auth.security.CustomAccessDeniedHandler
import com.gerd.domain.auth.security.CustomAuthenticationEntryPoint
import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.controller.AdminFoodPageController
import com.gerd.domain.food.service.AdminFoodService
import com.gerd.global.common.response.PageResponse
import com.gerd.global.config.properties.AdminProperties
import com.gerd.global.config.properties.SwaggerProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [AdminFoodPageController::class])
@Import(WebSecurityConfig::class)
@EnableConfigurationProperties(AdminProperties::class, SwaggerProperties::class)
@TestPropertySource(
    properties = [
        "admin.email=admin@gerd.com",
        "admin.secret=test-secret",
        "swagger.username=swagger",
        "swagger.password=swagger",
    ],
)
class BackofficeSecurityTest @Autowired constructor(
    context: WebApplicationContext,
) {

    private val mockMvc: MockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()

    @MockitoBean
    private lateinit var adminFoodService: AdminFoodService

    @MockitoBean
    private lateinit var jwtExceptionFilter: JwtExceptionFilter

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var customAccessDeniedHandler: CustomAccessDeniedHandler

    @MockitoBean
    private lateinit var customAuthenticationEntryPoint: CustomAuthenticationEntryPoint

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var accessTokenBlacklist: AccessTokenBlacklist

    @Test
    fun `로그인하지 않으면 음식 목록은 로그인 페이지로 이동한다`() {
        mockMvc.get("/admin/foods").andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/admin/login")
        }
    }

    @Test
    fun `로그인 페이지는 인증 없이 열린다`() {
        mockMvc.get("/admin/login").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `올바른 이메일과 시크릿으로 로그인하면 음식 목록으로 이동한다`() {
        mockMvc.post("/admin/login") {
            with(csrf())
            param("username", "admin@gerd.com")
            param("password", "test-secret")
        }.andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/admin/foods")
        }
    }

    @Test
    fun `잘못된 시크릿이면 로그인 실패로 이동한다`() {
        mockMvc.post("/admin/login") {
            with(csrf())
            param("username", "admin@gerd.com")
            param("password", "wrong")
        }.andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/admin/login?error")
        }
    }

    @Test
    fun `CSRF 토큰 없이 승격 요청하면 거부된다`() {
        whenever(adminFoodService.getAllUserFoods(any(), anyOrNull())).thenReturn(
            PageResponse(emptyList<com.gerd.domain.food.dto.AdminUserFoodDTO>(), 0, 100, 0, 0, false, true, true),
        )

        mockMvc.post("/admin/foods/uuid-1/promote").andExpect {
            status { isForbidden() }
        }
    }
}
