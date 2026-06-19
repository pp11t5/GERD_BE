package com.gerd.domain.meal.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.meal.dto.MealCandidatesDTO
import com.gerd.domain.meal.dto.MealFoodRecordDetailDTO
import com.gerd.domain.meal.dto.MealRecordDetailDTO
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.service.MealCommandService
import com.gerd.domain.meal.service.MealQueryService
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.fixture.MealRecordFixture
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
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [MealRecordController::class])
@AutoConfigureMockMvc(addFilters = false)
class MealRecordControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var mealCommandService: MealCommandService

    @MockitoBean
    private lateinit var mealQueryService: MealQueryService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @Nested
    inner class `POST meal-records` {

        @Test
        @WithCustomUser
        fun `식사 음식을 추가하면 상세를 반환한다`() {
            whenever(mealCommandService.create(any(), any())).thenReturn(mealFoodDetail())

            mockMvc.post("/api/v1/meal-records") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"foodExternalId":"${FoodFixture.EXTERNAL_ID}","eatenAt":"2026-06-11T12:30:00+09:00"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
                jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
                jsonPath("$.result.food.name") { value("된장찌개") }
            }
        }

        @Test
        @WithCustomUser
        fun `음식이 없으면 FOOD404_1`() {
            whenever(mealCommandService.create(any(), any()))
                .thenThrow(GeneralException(FoodErrorCode.FOOD_NOT_FOUND))

            mockMvc.post("/api/v1/meal-records") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"foodExternalId":"${FoodFixture.EXTERNAL_ID}"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("FOOD404_1") }
            }
        }
    }

    @Nested
    inner class `GET meal-records` {

        @Test
        @WithCustomUser
        fun `식사 음식 단건을 조회한다`() {
            whenever(mealQueryService.getDetail(any(), any())).thenReturn(mealFoodDetail())

            mockMvc.get("/api/v1/meal-records/foods/${MealRecordFixture.MEAL_FOOD_EXTERNAL_ID}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
                }
        }

        @Test
        @WithCustomUser
        fun `끼니 상세를 조회한다`() {
            whenever(mealQueryService.getGroupDetail(any(), any())).thenReturn(mealRecordDetail())

            mockMvc.get("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_ID}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.mealId") { value(MealRecordFixture.MEAL_RECORD_ID.toString()) }
                }
        }

        @Test
        @WithCustomUser
        fun `증상 미연결 후보를 조회한다`() {
            whenever(mealQueryService.getCandidates(any())).thenReturn(
                listOf(
                    MealCandidatesDTO(
                        date = "2026-06-11",
                        meals = listOf(
                            MealCandidatesDTO.MealCandidateItem(
                                mealRecordId = MealRecordFixture.MEAL_RECORD_ID.toString(),
                                representativeFood = MealCandidatesDTO.RepresentativeFood("된장찌개", "soup_stew"),
                                otherFoodCount = 1,
                                eatenAt = "2026-06-11T12:30:00+09:00",
                            ),
                        ),
                    ),
                ),
            )

            mockMvc.get("/api/v1/meal-records/candidates")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result[0].meals[0].mealRecordId") { value(MealRecordFixture.MEAL_RECORD_ID.toString()) }
                    jsonPath("$.result[0].meals[0].otherFoodCount") { value(1) }
                }
        }
    }

    @Nested
    inner class `DELETE meal-records` {

        @Test
        @WithCustomUser
        fun `식사 음식을 삭제한다`() {
            mockMvc.delete("/api/v1/meal-records/foods/${MealRecordFixture.MEAL_FOOD_EXTERNAL_ID}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.isSuccess") { value(true) }
                }
        }

        @Test
        @WithCustomUser
        fun `끼니가 없으면 MEAL404_2`() {
            whenever(mealCommandService.deleteMealRecord(any(), any()))
                .thenThrow(GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND))

            mockMvc.delete("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_ID}")
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("MEAL404_2") }
                }
        }
    }

    private fun mealFoodDetail() = MealFoodRecordDetailDTO(
        mealFoodId = MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(),
        eatenAt = "2026-06-11T12:30:00+09:00",
        food = MealFoodRecordDetailDTO.FoodInfoDTO(
            mealRecordExternalId = MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(),
            name = "된장찌개",
            category = "soup_stew",
        ),
        analysis = null,
        stateRecord = null,
    )

    private fun mealRecordDetail() = MealRecordDetailDTO(
        mealId = MealRecordFixture.MEAL_RECORD_ID.toString(),
        mealGroupId = MealRecordFixture.MEAL_RECORD_ID.toString(),
        eatenAt = "2026-06-11T12:30:00+09:00",
        memo = null,
        stateRecords = null,
    )
}
