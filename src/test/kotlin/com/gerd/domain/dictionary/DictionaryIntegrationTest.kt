package com.gerd.domain.dictionary

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DictionaryIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val mealRecordRepository: MealRecordRepository,
    private val mealFoodRepository: MealFoodRepository,
    private val symptomRepository: SymptomRepository,
    private val dictionaryRepository: UserFoodDictionaryRepository,
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
            "22222222-2222-2222-2222-222222222222",
            "dict-integration@test.com",
            "지원",
        )
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.update("DELETE FROM user_food_dictionaries")
        jdbcTemplate.update("DELETE FROM symptom_types")
        jdbcTemplate.update("DELETE FROM symptom_records")
        jdbcTemplate.update("DELETE FROM meal_foods")
        jdbcTemplate.update("DELETE FROM meal_records")
        foodRepository.deleteAll()
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", USER_ID)
    }

    @Nested
    inner class `증상 삭제 시 도감 항목 제거` {

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `comfortable 증상 삭제 시 연결된 SAFE 항목이 제거된다`() {
            val seeded = seedMeal()

            // comfortable 증상 생성 → SAFE 적재
            val createResult = mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-06-01T12:00:00+09:00",
                      "mealRecordId": "${seeded.record.externalId}"
                    }
                """.trimIndent()
            }.andExpect { status { isOk() } }.andReturn()

            val symptomId = com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResult.response.contentAsString)
                .at("/result/symptomId").asText()

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(1L)

            // 증상 삭제
            mockMvc.delete("/api/v1/symptoms/{id}", symptomId)
                .andExpect { status { isOk() } }

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(0L)
        }

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `uncomfortable 증상 삭제 시 SAFE 항목에 변화가 없다`() {
            val seeded = seedMeal()

            val createResult = mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "uncomfortable",
                      "symptomTypes": ["acid_reflux"],
                      "occurredAt": "2026-06-01T12:00:00+09:00",
                      "mealRecordId": "${seeded.record.externalId}"
                    }
                """.trimIndent()
            }.andExpect { status { isOk() } }.andReturn()

            val symptomId = com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResult.response.contentAsString)
                .at("/result/symptomId").asText()

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(0L)

            mockMvc.delete("/api/v1/symptoms/{id}", symptomId)
                .andExpect { status { isOk() } }

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(0L)
        }

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `같은 음식이 두 comfortable 증상에 연결되어 있으면 하나 삭제해도 SAFE 항목이 보존된다`() {
            val food = seedFood("공통음식")
            val record1 = seedMealWithFood(food)
            val record2 = seedMealWithFood(food)

            val user = userRepository.getReferenceById(USER_ID)

            // 두 끼니에 각각 comfortable 증상 생성 → 같은 음식이 SAFE에 한 번 적재
            val createResult1 = mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-06-01T12:00:00+09:00",
                      "mealRecordId": "${record1.externalId}"
                    }
                """.trimIndent()
            }.andExpect { status { isOk() } }.andReturn()

            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-06-01T13:00:00+09:00",
                      "mealRecordId": "${record2.externalId}"
                    }
                """.trimIndent()
            }.andExpect { status { isOk() } }

            // SAFE 적재 확인 (unique constraint 때문에 1건만)
            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(1L)

            val symptomId1 = com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResult1.response.contentAsString)
                .at("/result/symptomId").asText()

            // 첫 번째 증상 삭제 → 두 번째 comfortable 증상이 여전히 유효하므로 SAFE 항목 보존
            mockMvc.delete("/api/v1/symptoms/{id}", symptomId1)
                .andExpect { status { isOk() } }

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(1L)
        }
    }

    @Nested
    inner class `끼니 삭제 시 도감 항목 제거` {

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `comfortable 증상이 연결된 끼니 전체 삭제 시 SAFE 항목이 제거된다`() {
            val seeded = seedMeal()

            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-06-01T12:00:00+09:00",
                      "mealRecordId": "${seeded.record.externalId}"
                    }
                """.trimIndent()
            }.andExpect { status { isOk() } }

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(1L)

            // 끼니 전체 삭제
            mockMvc.delete("/api/v1/meal-records/{id}", seeded.record.externalId)
                .andExpect { status { isOk() } }

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(0L)
            assertThat(symptomRepository.findAll()).isEmpty()
            assertThat(mealFoodRepository.findAll()).isEmpty()
        }

        @Test
        @WithCustomUser(userId = USER_ID)
        fun `끼니의 마지막 음식을 단건 삭제하면 연결 증상의 SAFE 항목도 제거된다`() {
            val seeded = seedMeal()

            mockMvc.post("/api/v1/symptoms") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "symptomState": "comfortable",
                      "symptomTypes": [],
                      "occurredAt": "2026-06-01T12:00:00+09:00",
                      "mealRecordId": "${seeded.record.externalId}"
                    }
                """.trimIndent()
            }.andExpect { status { isOk() } }

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(1L)

            // 마지막 음식 단건 삭제 → 끼니도 함께 삭제되는 경로
            mockMvc.delete("/api/v1/meal-records/foods/{id}", seeded.mealFood.externalId)
                .andExpect { status { isOk() } }

            assertThat(dictionaryRepository.countByUser_IdAndDictionaryType(USER_ID, DictionaryType.SAFE)).isEqualTo(0L)
        }
    }

    private fun seedFood(name: String): Food =
        foodRepository.save(Food(name = name, source = FoodSource.SEED, visibility = FoodVisibility.PUBLIC))

    private fun seedMeal(): SeededMeal {
        val food = seedFood("통합테스트음식")
        return seedMealWithFoodWrapped(food)
    }

    private fun seedMealWithFood(food: Food): MealRecord {
        return seedMealWithFoodWrapped(food).record
    }

    private fun seedMealWithFoodWrapped(food: Food): SeededMeal {
        val user = userRepository.getReferenceById(USER_ID)
        val record = mealRecordRepository.save(
            MealRecord(user = user, eatenAt = LocalDateTime.of(2026, 6, 1, 12, 0, 0)),
        )
        val mealFood = mealFoodRepository.save(
            MealFood(
                user = user,
                foodId = food.id!!,
                mealRecord = record,
                eatenAt = record.eatenAt,
            ),
        )
        return SeededMeal(record, mealFood)
    }

    private data class SeededMeal(
        val record: MealRecord,
        val mealFood: MealFood,
    )

    companion object {
        private const val USER_ID = 2L
    }
}
