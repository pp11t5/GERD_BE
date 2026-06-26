package com.gerd.domain.meal

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.FoodCategory
import com.gerd.domain.food.entity.FoodCategoryMap
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodCategoryRepository
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.judgment.dto.LlmJudgmentDTO
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.judgment.service.JudgmentGeminiAdapter
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.global.security.WithCustomUser
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MealRecordIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val foodRepository: FoodRepository,
    private val foodCategoryRepository: FoodCategoryRepository,
    private val foodCategoryMapRepository: FoodCategoryMapRepository,
    private val mealFoodRepository: MealFoodRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @MockitoBean
    private lateinit var judgmentGeminiAdapter: JudgmentGeminiAdapter

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update(
            """
            INSERT INTO users (user_id, external_id, email, role, status)
            VALUES (?, ?::uuid, ?, 'USER', 'ACTIVE')
            ON CONFLICT (user_id) DO NOTHING
            """.trimIndent(),
            USER_ID,
            "11111111-1111-1111-1111-111111111111",
            "meal-integration@test.com",
        )
    }

    @AfterEach
    fun tearDown() {
        mealFoodRepository.deleteAll()
        mealRecordRepository.deleteAll()
        foodCategoryMapRepository.deleteAll()
        foodCategoryRepository.deleteAll()
        foodRepository.deleteAll()
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", USER_ID)
    }

    @Nested
    inner class `검색-분석-기록-조회 흐름` {

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `음식 검색 후 분석 결과가 캐싱되고 식사 기록 조회 시 증상이 없으면 null을 반환한다`() {
            val food = seedFood()
            whenever(judgmentGeminiAdapter.generateJudgment(any(), any(), any())).thenReturn(llmJudgment())

            mockMvc.get("/api/v1/foods/search") {
                param("q", "통합")
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.result[0].externalId") { value(food.externalId.toString()) }
                jsonPath("$.result[0].name") { value("통합 된장찌개") }
                jsonPath("$.result[0].category") { value("soup_stew") }
            }

            mockMvc.get("/api/v1/foods/{foodExternalId}/judgment", food.externalId)
                .andExpect {
                    status { isOk() }
                    header { string("X-Cache", "MISS") }
                    jsonPath("$.result.grade") { value("CAUTION") }
                    jsonPath("$.result.personalTitle") { value("속이 불편할 수 있어요") }
                    jsonPath("$.result.items[0].emphasis") { value("맵고 짤 수 있어요") }
                }

            mockMvc.get("/api/v1/foods/{foodExternalId}/judgment", food.externalId)
                .andExpect {
                    status { isOk() }
                    header { string("X-Cache", "HIT") }
                    jsonPath("$.result.grade") { value("CAUTION") }
                    jsonPath("$.result.personalTitle") { value("속이 불편할 수 있어요") }
                }

            verify(judgmentGeminiAdapter, times(1)).generateJudgment(any(), any(), any())

            mockMvc.post("/api/v1/meal-records/foods/{foodId}", food.externalId) {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "eatenAt": "2026-06-11T12:30:00+09:00"
                    }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.result.food.name") { value("통합 된장찌개") }
                jsonPath("$.result.analysis.judgmentGrade") { value("CAUTION") }
                jsonPath("$.result.analysis.triggerAnalysis.ment") { value("맵고 짤 수 있어요") }
                jsonPath("$.result.analysis.triggerAnalysis.content") { value("자극적인 국물은 역류 증상을 유발할 수 있어요.") }
                jsonPath("$.result.analysis.allergyAnalysis.ment") { value("정확한 성분은 성분표를 확인해 보세요") }
                jsonPath("$.result.stateRecord") { value(nullValue()) }
            }

            val mealFood = mealFoodRepository.findAll().single()
            mockMvc.get("/api/v1/meal-records/foods/{mealFoodId}", mealFood.externalId)
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.mealFoodId") { value(mealFood.externalId.toString()) }
                    jsonPath("$.result.food.name") { value("통합 된장찌개") }
                    jsonPath("$.result.analysis.judgmentGrade") { value("CAUTION") }
                    jsonPath("$.result.analysis.triggerAnalysis.ment") { value("맵고 짤 수 있어요") }
                    jsonPath("$.result.analysis.allergyAnalysis.content") { value("알레르겐 정보가 부족하면 성분표 확인이 필요해요.") }
                    jsonPath("$.result.stateRecord") { value(nullValue()) }
                }

            val mealRecord = mealRecordRepository.findById(mealFood.mealRecordId).orElseThrow()
            mockMvc.get("/api/v1/meal-records/{mealRecordId}", mealRecord.externalId)
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.mealRecordId") { value(mealRecord.externalId.toString()) }
                    jsonPath("$.result.meals[0].mealFoodId") { value(mealFood.externalId.toString()) }
                    jsonPath("$.result.meals[0].name") { value("통합 된장찌개") }
                    jsonPath("$.result.meals[0].category") { value("soup_stew") }
                    jsonPath("$.result.stateRecords") { value(nullValue()) }
                }
        }
    }

    private fun seedFood(): Food {
        val food = foodRepository.save(
            Food(
                name = "통합 된장찌개",
                source = FoodSource.SEED,
                visibility = FoodVisibility.PUBLIC,
                description = "맵고 짠 국물 음식",
            ),
        )
        val category = foodCategoryRepository.save(
            FoodCategory(
                code = "soup_stew",
                displayName = "국/찌개",
                sortOrder = 1,
            ),
        )
        foodCategoryMapRepository.save(FoodCategoryMap(food = food, foodCategory = category))
        return foodRepository.findById(food.id!!).orElseThrow()
    }

    private fun llmJudgment() = LlmJudgmentDTO(
        grade = JudgmentGrade.CAUTION,
        personalTitle = "속이 불편할 수 있어요",
        items = listOf(
            LlmJudgmentDTO.LlmJudgmentItemDTO(
                emphasis = "맵고 짤 수 있어요",
                body = "자극적인 국물은 역류 증상을 유발할 수 있어요.",
            ),
            LlmJudgmentDTO.LlmJudgmentItemDTO(
                emphasis = "정확한 성분은 성분표를 확인해 보세요",
                body = "알레르겐 정보가 부족하면 성분표 확인이 필요해요.",
            ),
        ),
    )

    companion object {
        private const val USER_ID = 1L
    }
}
