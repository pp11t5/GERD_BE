package com.gerd.domain.symptom

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.FoodCategory
import com.gerd.domain.food.entity.FoodCategoryMap
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.repository.FoodCategoryMapRepository
import com.gerd.domain.food.repository.FoodCategoryRepository
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.meal.entity.MealFood
import com.gerd.domain.meal.entity.MealRecord
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.entity.Symptom
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.domain.symptom.service.SymptomPatternRefreshService
import com.gerd.global.security.WithCustomUser
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SymptomIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val foodCategoryRepository: FoodCategoryRepository,
    private val foodCategoryMapRepository: FoodCategoryMapRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val mealFoodRepository: MealFoodRepository,
    private val symptomRepository: SymptomRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @MockitoBean
    private lateinit var symptomPatternRefreshService: SymptomPatternRefreshService

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update(
            """
            INSERT INTO users (user_id, external_id, email, nickname, role, status)
            VALUES (?, ?::uuid, ?, ?, 'USER', 'ACTIVE')
            ON CONFLICT (user_id) DO NOTHING
            """.trimIndent(),
            USER_ID,
            "11111111-1111-1111-1111-111111111111",
            "symptom-integration@test.com",
            "지원",
        )
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.update("DELETE FROM symptom_types")
        jdbcTemplate.update("DELETE FROM symptom_records")
        jdbcTemplate.update("DELETE FROM meal_foods")
        jdbcTemplate.update("DELETE FROM meal_records")
        foodCategoryMapRepository.deleteAll()
        foodCategoryRepository.deleteAll()
        foodRepository.deleteAll()
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", USER_ID)
    }

    @Nested
    inner class `증상 기록 흐름` {

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `식사에 연결한 증상을 생성 조회 수정 메모수정 삭제한다`() {
            val meal = seedMeal()

            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-05-12T19:30:00+09:00",
                      "mealRecordId": "${meal.record.externalId}",
                      "memo": "속이 편했어요"
                    }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.code") { value("COMMON200") }
                jsonPath("$.result.symptomId") { exists() }
                jsonPath("$.result.symptomState") { value("comfortable") }
                jsonPath("$.result.linkedMeal.mealRecordId") { value(meal.record.externalId.toString()) }
                jsonPath("$.result.linkedMeal.foods[0].mealFoodId") { value(meal.food.externalId.toString()) }
                jsonPath("$.result.linkedMeal.foods[0].name") { value("통합 된장찌개") }
                jsonPath("$.result.linkedMeal.foods[0].category") { value("soup_stew") }
                jsonPath("$.result.memo") { doesNotExist() }
                jsonPath("$.result.analysis") { value(nullValue()) }
            }

            val symptom = symptomRepository.findAll().single()
            val symptomExternalId = symptom.externalId.toString()
            verify(symptomPatternRefreshService, times(1)).refreshAsync(symptomExternalId, USER_ID)

            mockMvc.get("/api/v1/symptoms/{symptomId}", symptomExternalId)
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result.symptomId") { value(symptomExternalId) }
                    jsonPath("$.result.linkedMeal.foods[0].name") { value("통합 된장찌개") }
                    jsonPath("$.result.memo") { doesNotExist() }
                }

            mockMvc.put("/api/v1/symptoms/{symptomId}", symptomExternalId) {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "uncomfortable",
                      "symptomTypes": ["acid_reflux"],
                      "occurredAt": "2026-05-12T20:00:00+09:00",
                      "mealRecordId": "${meal.record.externalId}",
                      "memo": "신물이 올라왔어요"
                    }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
                jsonPath("$.result") { value(nullValue()) }
            }

            val updated = symptomRepository.findByExternalIdAndUser_Id(symptom.externalId!!, USER_ID)!!
            assertThat(updated.symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
            assertThat(updated.symptomTypes.map { it.code }).containsExactly("acid_reflux")
            assertThat(updated.isAnalysisDirty).isTrue()
            assertThat(updated.analysisVersion).isEqualTo(1L)

            mockMvc.patch("/api/v1/symptoms/{symptomId}/memo", symptomExternalId) {
                contentType = MediaType.APPLICATION_JSON
                content = """{"memo":"  메모만 바꿨어요  "}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.result") { value(nullValue()) }
            }

            val memoUpdated = symptomRepository.findByExternalIdAndUser_Id(symptom.externalId!!, USER_ID)!!
            assertThat(memoUpdated.memo).isEqualTo("메모만 바꿨어요")
            assertThat(memoUpdated.analysisVersion).isEqualTo(2L)
            verify(symptomPatternRefreshService, times(4)).refreshAsync(symptomExternalId, USER_ID)

            mockMvc.delete("/api/v1/symptoms/{symptomId}", symptomExternalId)
                .andExpect {
                    status { isOk() }
                    jsonPath("$.result") { value(nullValue()) }
                }

            assertThat(symptomRepository.findByExternalIdAndUser_Id(symptom.externalId!!, USER_ID)).isNull()
        }

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `존재하지 않는 끼니에 증상을 생성하면 MEAL_RECORD_NOT_FOUND`() {
            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-05-12T19:30:00+09:00",
                      "mealRecordId": "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f"
                    }
                """.trimIndent()
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("MEAL404_2") }
            }

            assertThat(symptomRepository.findAll()).isEmpty()
            verify(symptomPatternRefreshService, times(0)).refreshAsync(any(), any())
        }

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `새 증상을 생성해도 기존 증상 분석 상태는 dirty로 바꾸지 않는다`() {
            val meal = seedMeal()
            val existing = symptomRepository.save(
                Symptom(
                    user = userRepository.getReferenceById(USER_ID),
                    symptomState = SymptomState.COMFORTABLE,
                    symptomTypes = emptySet(),
                    occurredAt = LocalDateTime.of(2026, 5, 10, 19, 0, 0),
                    mealRecordId = meal.record.id!!,
                    analysisJson = """{"label":"유지 권장"}""",
                    isAnalysisDirty = false,
                    analysisVersion = 3L,
                ),
            )

            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "uncomfortable",
                      "symptomTypes": ["acid_reflux"],
                      "occurredAt": "2026-05-12T19:30:00+09:00",
                      "mealRecordId": "${meal.record.externalId}"
                    }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.isSuccess") { value(true) }
            }

            val unchanged = symptomRepository.findById(existing.id!!).orElseThrow()
            assertThat(unchanged.isAnalysisDirty).isFalse()
            assertThat(unchanged.analysisVersion).isEqualTo(3L)
            assertThat(unchanged.analysisJson).contains("유지 권장")

            val created = symptomRepository.findAll().single { it.id != existing.id }
            verify(symptomPatternRefreshService, times(1)).refreshAsync(created.externalId.toString(), USER_ID)
        }
    }

    private fun seedMeal(): SeededMeal {
        val food = foodRepository.save(
            Food(
                name = "통합 된장찌개",
                source = FoodSource.SEED,
                visibility = FoodVisibility.PUBLIC,
                description = "국물 음식",
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

        val user = userRepository.getReferenceById(USER_ID)
        val record = mealRecordRepository.save(
            MealRecord(
                user = user,
                eatenAt = LocalDateTime.of(2026, 5, 12, 18, 0, 0),
            ),
        )
        val mealFood = mealFoodRepository.save(
            MealFood(
                user = user,
                foodId = food.id!!,
                mealRecordId = record.id!!,
                eatenAt = record.eatenAt,
            ),
        )
        return SeededMeal(record, mealFood)
    }

    private data class SeededMeal(
        val record: MealRecord,
        val food: MealFood,
    )

    companion object {
        private const val USER_ID = 1L
    }
}
