package com.gerd.domain.meal.controller

import com.gerd.domain.auth.security.JwtProvider
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.dto.MealAnalysisSnapshotDTO
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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
    inner class `POST meal-records - 신규 끼니 (ID)` {

        @Test
        @WithCustomUser
        fun `음식 ID로 신규 끼니를 생성하면 상세를 반환한다`() {
            whenever(mealCommandService.createNew(any(), any(), any())).thenReturn(mealFoodDetail())

            mockMvc.post("/api/v1/meal-records/foods/${FoodFixture.EXTERNAL_ID}") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"eatenAt":"2026-06-11T12:30:00+09:00"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
                jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
                jsonPath("$.result.food.name") { value("된장찌개") }
                jsonPath("$.result.analysis.judgmentGrade") { value("CAUTION") }
            }

            verify(mealCommandService).createNew(
                FoodFixture.EXTERNAL_ID.toString(),
                "2026-06-11T12:30:00+09:00",
                1L,
            )
        }

        @Test
        @WithCustomUser
        fun `음식 ID는 path variable로 받고 body가 없으면 먹은 시각 null로 전달한다`() {
            whenever(mealCommandService.createNew(any(), anyOrNull(), any())).thenReturn(mealFoodDetail())

            mockMvc.post("/api/v1/meal-records/foods/${FoodFixture.EXTERNAL_ID}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
                }

            verify(mealCommandService).createNew(FoodFixture.EXTERNAL_ID.toString(), null, 1L)
        }

        @Test
        @WithCustomUser
        fun `음식이 없으면 FOOD404_1`() {
            whenever(mealCommandService.createNew(any(), anyOrNull(), any()))
                .thenThrow(GeneralException(FoodErrorCode.FOOD_NOT_FOUND))

            mockMvc.post("/api/v1/meal-records/foods/${FoodFixture.EXTERNAL_ID}")
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("FOOD404_1") }
                }
        }

        @Test
        @WithCustomUser
        fun `먹은 시각이 offset 없는 형식이면 400`() {
            mockMvc.post("/api/v1/meal-records/foods/${FoodFixture.EXTERNAL_ID}") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"eatenAt":"2026-06-11T12:30:00"}"""
            }.andExpect {
                status { isBadRequest() }
            }

            verify(mealCommandService, never()).createNew(any(), any(), any())
        }
    }

    @Nested
    inner class `POST meal-records - 신규 끼니 (text)` {

        @Test
        @WithCustomUser
        fun `음식 이름으로 신규 끼니를 생성하면 상세를 반환한다`() {
            whenever(mealCommandService.createNewByText(any(), any(), any())).thenReturn(mealFoodDetail())

            mockMvc.post("/api/v1/meal-records") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"엄마표 김치찌개","eatenAt":"2026-06-11T12:30:00+09:00"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
            }

            verify(mealCommandService).createNewByText(
                "엄마표 김치찌개",
                "2026-06-11T12:30:00+09:00",
                1L,
            )
        }

        @Test
        @WithCustomUser
        fun `name 필드가 없으면 400`() {
            mockMvc.post("/api/v1/meal-records") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"eatenAt":"2026-06-11T12:30:00+09:00"}"""
            }.andExpect { status { isBadRequest() } }

            verify(mealCommandService, never()).createNewByText(any(), any(), any())
        }
    }

    @Nested
    inner class `POST meal-records - 같이 먹은 (ID)` {

        @Test
        @WithCustomUser
        fun `기존 끼니에 음식 ID로 추가하면 상세를 반환한다`() {
            whenever(mealCommandService.append(any(), any(), any(), any())).thenReturn(mealFoodDetail())

            mockMvc.post("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}/foods/${FoodFixture.EXTERNAL_ID}") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"eatenAt":"2026-06-11T12:30:00+09:00"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
            }

            verify(mealCommandService).append(
                MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
                FoodFixture.EXTERNAL_ID.toString(),
                "2026-06-11T12:30:00+09:00",
                1L,
            )
        }

        @Test
        @WithCustomUser
        fun `끼니 ID와 음식 ID는 path variable로 받고 body가 없으면 먹은 시각 null로 전달한다`() {
            whenever(mealCommandService.append(any(), any(), anyOrNull(), any())).thenReturn(mealFoodDetail())

            mockMvc.post("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}/foods/${FoodFixture.EXTERNAL_ID}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
                }

            verify(mealCommandService).append(
                MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
                FoodFixture.EXTERNAL_ID.toString(),
                null,
                1L,
            )
        }

        @Test
        @WithCustomUser
        fun `끼니가 없으면 MEAL404_2`() {
            whenever(mealCommandService.append(any(), any(), anyOrNull(), any()))
                .thenThrow(GeneralException(MealErrorCode.MEAL_RECORD_NOT_FOUND))

            mockMvc.post("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}/foods/${FoodFixture.EXTERNAL_ID}")
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("MEAL404_2") }
                }
        }
    }

    @Nested
    inner class `POST meal-records - 같이 먹은 (text)` {

        @Test
        @WithCustomUser
        fun `기존 끼니에 음식 이름으로 추가하면 상세를 반환한다`() {
            whenever(mealCommandService.appendByText(any(), any(), anyOrNull(), any())).thenReturn(mealFoodDetail())

            mockMvc.post("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}/foods") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"엄마표 김치찌개"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
            }

            verify(mealCommandService).appendByText(
                MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
                "엄마표 김치찌개",
                null,
                1L,
            )
        }

        @Test
        @WithCustomUser
        fun `name 필드가 없으면 400`() {
            mockMvc.post("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}/foods") {
                contentType = MediaType.APPLICATION_JSON
                content = """{}"""
            }.andExpect { status { isBadRequest() } }

            verify(mealCommandService, never()).appendByText(any(), any(), any(), any())
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
                    jsonPath("$.isSuccess") { value(true) }
                    jsonPath("$.code") { value("COMMON200") }
                    jsonPath("$.result.mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
                    jsonPath("$.result.analysis.judgmentGrade") { value("CAUTION") }
                    jsonPath("$.result.analysis.triggerAnalysis.ment") { value("맵고 짤 수 있어요") }
                    jsonPath("$.result.analysis.allergyAnalysis.content") { value("성분표 확인이 필요해요") }
                }
        }

        @Test
        @WithCustomUser
        fun `끼니 상세를 조회한다`() {
            whenever(mealQueryService.getGroupDetail(any(), any())).thenReturn(mealRecordDetail())

            mockMvc.get("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.mealRecordId") { value(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString()) }
                    jsonPath("$.result.meals[0].mealFoodId") { value(MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString()) }
                    jsonPath("$.result.meals[0].name") { value("된장찌개") }
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
                                mealRecordId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
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
                    jsonPath("$.result[0].meals[0].mealRecordId") { value(MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString()) }
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

            mockMvc.delete("/api/v1/meal-records/${MealRecordFixture.MEAL_RECORD_EXTERNAL_ID}")
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
            mealRecordExternalId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
            name = "된장찌개",
            category = "soup_stew",
        ),
        analysis = mealAnalysis(),
        stateRecord = null,
    )

    private fun mealAnalysis() = MealAnalysisSnapshotDTO(
        judgmentGrade = JudgmentGrade.CAUTION,
        triggerAnalysis = MealAnalysisSnapshotDTO.AnalysisItemDTO(
            ment = "맵고 짤 수 있어요",
            content = "천천히 드세요",
        ),
        allergyAnalysis = MealAnalysisSnapshotDTO.AnalysisItemDTO(
            ment = "알레르기 성분을 확인해 주세요",
            content = "성분표 확인이 필요해요",
        ),
    )

    private fun mealRecordDetail() = MealRecordDetailDTO(
        mealRecordId = MealRecordFixture.MEAL_RECORD_EXTERNAL_ID.toString(),
        eatenAt = "2026-06-11T12:30:00+09:00",
        meals = listOf(
            MealRecordDetailDTO.MealFoodDetailDTO(
                mealFoodId = MealRecordFixture.MEAL_FOOD_EXTERNAL_ID.toString(),
                name = "된장찌개",
                category = "soup_stew",
                eatenAt = "2026-06-11T12:30:00+09:00",
            ),
        ),
        stateRecords = null,
    )
}
