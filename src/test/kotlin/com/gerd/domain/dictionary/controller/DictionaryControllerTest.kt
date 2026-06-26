package com.gerd.domain.dictionary.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.dictionary.dto.CautionRiskFoodItemDTO
import com.gerd.domain.dictionary.dto.DictionaryCountResponseDTO
import com.gerd.domain.dictionary.dto.SafeFoodItemDTO
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.service.DictionaryQueryService
import com.gerd.global.common.response.CursorResponse
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [DictionaryController::class])
@AutoConfigureMockMvc(addFilters = false)
class DictionaryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var dictionaryService: DictionaryQueryService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @Nested
    inner class `GET dictionary count` {

        @Test
        @WithCustomUser
        fun `안전·주의위험 탭 카운트를 반환한다`() {
            whenever(dictionaryService.getCount(any()))
                .thenReturn(DictionaryCountResponseDTO(safeCount = 5, cautionRiskCount = 3))

            mockMvc.get("/api/v1/dictionary/count")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.safeCount") { value(5) }
                    jsonPath("$.result.cautionRiskCount") { value(3) }
                }
        }

        @Test
        @WithCustomUser
        fun `항목이 없으면 0을 반환한다`() {
            whenever(dictionaryService.getCount(any()))
                .thenReturn(DictionaryCountResponseDTO(safeCount = 0, cautionRiskCount = 0))

            mockMvc.get("/api/v1/dictionary/count")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.safeCount") { value(0) }
                    jsonPath("$.result.cautionRiskCount") { value(0) }
                }
        }
    }

    @Nested
    inner class `GET dictionary safe` {

        @Test
        @WithCustomUser
        fun `안전 음식 목록을 커서 페이징으로 반환한다`() {
            whenever(dictionaryService.getSafeFoods(anyOrNull(), anyOrNull(), any()))
                .thenReturn(safeFoodResponse(hasNext = true, nextCursor = 9L))

            mockMvc.get("/api/v1/dictionary/safe")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.items[0].foodId") { value(FoodFixture.EXTERNAL_ID.toString()) }
                    jsonPath("$.result.items[0].name") { value("된장찌개") }
                    jsonPath("$.result.items[0].code") { value("soup_stew") }
                    jsonPath("$.result.items[0].type") { doesNotExist() }
                    jsonPath("$.result.hasNext") { value(true) }
                    jsonPath("$.result.nextCursor") { value(9) }
                }
        }

        @Test
        @WithCustomUser
        fun `마지막 페이지면 hasNext false, nextCursor null이다`() {
            whenever(dictionaryService.getSafeFoods(anyOrNull(), anyOrNull(), any()))
                .thenReturn(safeFoodResponse(hasNext = false, nextCursor = null))

            mockMvc.get("/api/v1/dictionary/safe")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.hasNext") { value(false) }
                    jsonPath("$.result.nextCursor") { doesNotExist() }
                }
        }

        @Test
        @WithCustomUser
        fun `cursor와 size 파라미터를 서비스에 전달한다`() {
            whenever(dictionaryService.getSafeFoods(eq(10), eq(5L), any()))
                .thenReturn(safeFoodResponse())

            mockMvc.get("/api/v1/dictionary/safe?cursor=5&size=10")
                .andExpect { status { isOk() } }

            verify(dictionaryService).getSafeFoods(eq(10), eq(5L), any())
        }
    }

    @Nested
    inner class `GET dictionary caution-risk` {

        @Test
        @WithCustomUser
        fun `주의·위험 음식 목록을 반환한다`() {
            val items = listOf(
                cautionRiskItem(type = DictionaryType.CAUTION),
                cautionRiskItem(type = DictionaryType.RISK),
            )
            whenever(dictionaryService.getCautionRiskFoods(anyOrNull(), anyOrNull(), any()))
                .thenReturn(CursorResponse(items = items, nextCursor = null, hasNext = false))

            mockMvc.get("/api/v1/dictionary/caution-risk")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.items[0].type") { value("caution") }
                    jsonPath("$.result.items[1].type") { value("risk") }
                    jsonPath("$.result.hasNext") { value(false) }
                }
        }

        @Test
        @WithCustomUser
        fun `cursor 파라미터를 서비스에 전달한다`() {
            whenever(dictionaryService.getCautionRiskFoods(anyOrNull(), eq(20L), any()))
                .thenReturn(CursorResponse(items = emptyList(), nextCursor = null, hasNext = false))

            mockMvc.get("/api/v1/dictionary/caution-risk?cursor=20")
                .andExpect { status { isOk() } }

            verify(dictionaryService).getCautionRiskFoods(anyOrNull(), eq(20L), any())
        }
    }

    private fun safeItem() = SafeFoodItemDTO(
        foodId = FoodFixture.EXTERNAL_ID.toString(),
        name = "된장찌개",
        code = "soup_stew",
    )

    private fun cautionRiskItem(type: DictionaryType) = CautionRiskFoodItemDTO(
        foodId = FoodFixture.EXTERNAL_ID.toString(),
        name = "된장찌개",
        code = "soup_stew",
        type = type,
    )

    private fun safeFoodResponse(
        hasNext: Boolean = false,
        nextCursor: Long? = null,
    ) = CursorResponse(
        items = listOf(safeItem()),
        nextCursor = nextCursor,
        hasNext = hasNext,
    )
}
