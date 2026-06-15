package com.gerd.domain.meal.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.dto.FoodSummaryDTO
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.dto.MealGroupDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordSummaryDTO
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.service.MealRecordCommandService
import com.gerd.domain.meal.service.MealRecordQueryService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.security.WithCustomUser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [MealController::class])
@AutoConfigureMockMvc(addFilters = false)
class MealControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean private lateinit var mealRecordCommandService: MealRecordCommandService
    @MockitoBean private lateinit var mealRecordQueryService: MealRecordQueryService
    @MockitoBean private lateinit var jwtProvider: JwtProvider

    private val mealId = "7f3a0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f"
    private val groupId = "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f"
    private val foodId = "9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f"

    private fun summary(grade: JudgmentGrade? = JudgmentGrade.RECOMMEND) = MealRecordSummaryDTO(
        mealId = mealId,
        mealGroupId = groupId,
        eatenAt = "2026-06-11T12:30:00+09:00",
        food = FoodSummaryDTO(foodId, "된장찌개", "soup_stew"),
        judgedGrade = grade,
    )

    private fun detail() = MealRecordDetailDTO(
        mealId = mealId,
        mealGroupId = groupId,
        eatenAt = "2026-06-11T12:30:00+09:00",
        memo = "메모",
        judgedGrade = JudgmentGrade.RECOMMEND,
        food = MealRecordDetailDTO.MealFoodDetailDTO(foodId, "된장찌개", "soup_stew", "저자극 한식"),
        stateRecords = emptyList(),
    )

    @Nested
    inner class `생성` {

        @Test
        @WithCustomUser
        fun `식사 기록을 생성하고 등급 스냅샷을 반환한다`() {
            whenever(mealRecordCommandService.create(any(), any())).thenReturn(summary())

            mockMvc.post("/api/v1/meals") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"foodExternalId":"$foodId","judgedGrade":"RECOMMEND"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
                jsonPath("$.result.mealId") { value(mealId) }
                jsonPath("$.result.mealGroupId") { value(groupId) }
                jsonPath("$.result.food.category") { value("soup_stew") }
                jsonPath("$.result.judgedGrade") { value("RECOMMEND") }
            }
        }

        @Test
        @WithCustomUser
        fun `judgedGrade 미전달도 허용한다`() {
            whenever(mealRecordCommandService.create(any(), any())).thenReturn(summary(grade = null))

            mockMvc.post("/api/v1/meals") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"foodExternalId":"$foodId"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.judgedGrade") { doesNotExist() } // null은 NON_NULL로 직렬화 제외
            }
        }

        @Test
        @WithCustomUser
        fun `음식이 없으면 FOOD404_1`() {
            whenever(mealRecordCommandService.create(any(), any()))
                .thenThrow(GeneralException(FoodErrorCode.FOOD_NOT_FOUND))

            mockMvc.post("/api/v1/meals") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"foodExternalId":"$foodId"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("FOOD404_1") }
            }
        }

        @Test
        @WithCustomUser
        fun `끼니가 없으면 MEAL404_2`() {
            whenever(mealRecordCommandService.create(any(), any()))
                .thenThrow(GeneralException(MealErrorCode.MEAL_GROUP_NOT_FOUND))

            mockMvc.post("/api/v1/meals") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"foodExternalId":"$foodId","mealGroupId":"$groupId"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("MEAL404_2") }
            }
        }

        @Test
        @WithCustomUser
        fun `eatenAt 형식이 잘못되면 MEAL400_2`() {
            whenever(mealRecordCommandService.create(any(), any()))
                .thenThrow(GeneralException(MealErrorCode.INVALID_DATE_TIME))

            mockMvc.post("/api/v1/meals") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"foodExternalId":"$foodId","eatenAt":"2026-06-11 12:30"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("MEAL400_2") }
            }
        }
    }

    @Nested
    inner class `단건 조회` {

        @Test
        @WithCustomUser
        fun `상세를 반환하고 상태 기록은 빈 배열이다`() {
            whenever(mealRecordQueryService.getDetail(any(), any())).thenReturn(detail())

            mockMvc.get("/api/v1/meals/$mealId").andExpect {
                status { isOk() }
                jsonPath("$.result.memo") { value("메모") }
                jsonPath("$.result.food.description") { value("저자극 한식") }
                jsonPath("$.result.stateRecords.length()") { value(0) }
            }
        }

        @Test
        @WithCustomUser
        fun `기록이 없으면 MEAL404_1`() {
            whenever(mealRecordQueryService.getDetail(any(), any()))
                .thenThrow(GeneralException(MealErrorCode.MEAL_NOT_FOUND))

            mockMvc.get("/api/v1/meals/$mealId").andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("MEAL404_1") }
            }
        }
    }

    @Nested
    inner class `메모 수정` {

        @Test
        @WithCustomUser
        fun `메모를 수정하고 상세 형태로 반환한다`() {
            whenever(mealRecordCommandService.updateMemo(any(), any(), any())).thenReturn(detail())

            mockMvc.patch("/api/v1/meals/$mealId") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"memo":"메모"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.memo") { value("메모") }
            }
        }

        @Test
        @WithCustomUser
        fun `메모가 200자를 초과하면 MEAL400_1`() {
            whenever(mealRecordCommandService.updateMemo(any(), any(), any()))
                .thenThrow(GeneralException(MealErrorCode.MEMO_TOO_LONG))

            mockMvc.patch("/api/v1/meals/$mealId") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"memo":"too long"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("MEAL400_1") }
            }
        }
    }

    @Nested
    inner class `삭제` {

        @Test
        @WithCustomUser
        fun `삭제에 성공하면 200을 반환한다`() {
            mockMvc.delete("/api/v1/meals/$mealId").andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
            }
        }

        @Test
        @WithCustomUser
        fun `이미 삭제된 기록이면 MEAL404_1`() {
            whenever(mealRecordCommandService.delete(any(), any()))
                .thenThrow(GeneralException(MealErrorCode.MEAL_NOT_FOUND))

            mockMvc.delete("/api/v1/meals/$mealId").andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("MEAL404_1") }
            }
        }
    }

    @Nested
    inner class `날짜별 조회` {

        @Test
        @WithCustomUser
        fun `끼니 그룹 목록을 반환한다`() {
            val group = MealGroupDTO(groupId, "2026-06-11T12:30:00+09:00", listOf(summary()))
            whenever(mealRecordQueryService.getDaily(any(), any())).thenReturn(listOf(group))

            mockMvc.get("/api/v1/meals?date=2026-06-11").andExpect {
                status { isOk() }
                jsonPath("$.result.length()") { value(1) }
                jsonPath("$.result[0].mealGroupId") { value(groupId) }
                jsonPath("$.result[0].records.length()") { value(1) }
            }
        }

        @Test
        @WithCustomUser
        fun `date 형식이 잘못되면 MEAL400_2`() {
            whenever(mealRecordQueryService.getDaily(any(), any()))
                .thenThrow(GeneralException(MealErrorCode.INVALID_DATE_TIME))

            mockMvc.get("/api/v1/meals?date=2026/06/11").andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("MEAL400_2") }
            }
        }
    }
}
