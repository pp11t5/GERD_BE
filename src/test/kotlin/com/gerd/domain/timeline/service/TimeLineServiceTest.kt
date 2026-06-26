package com.gerd.domain.timeline.service

import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.domain.meal.repository.MealFoodRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.symptom.entity.enums.SymptomState
import com.gerd.domain.symptom.entity.enums.SymptomType
import com.gerd.domain.symptom.repository.SymptomRepository
import com.gerd.domain.timeline.dto.TimeLineItemDTO
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.fixture.MealRecordFixture
import com.gerd.global.fixture.SymptomFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TimeLineServiceTest {

    @Mock private lateinit var symptomRepository: SymptomRepository
    @Mock private lateinit var mealRecordRepository: MealRecordRepository
    @Mock private lateinit var mealFoodRepository: MealFoodRepository
    @Mock private lateinit var foodRepository: FoodRepository

    private val service by lazy {
        TimeLineService(symptomRepository, mealRecordRepository, mealFoodRepository, foodRepository)
    }

    private val userId = 1L
    private val date = LocalDate.of(2026, 6, 17)

    @Nested
    inner class `일별 타임라인 조회` {

        @Test
        fun `식사 기록과 증상이 모두 없으면 빈 리스트를 반환한다`() {
            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(emptyList())
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(emptyList())

            val result = service.getTimeLine(userId, date)

            assertThat(result.items).isEmpty()
        }

        @Test
        fun `음식이 1개인 식사 기록은 Single로 매핑된다`() {
            val mealRecord = MealRecordFixture.mealRecord()
            val mealFood = MealRecordFixture.mealFood(foodId = 1L, judgedGrade = JudgmentGrade.RECOMMEND)
            val food = FoodFixture.food(id = 1L, name = "된장찌개")

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(mealRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(emptyList())
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(mealFood))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(food))

            val result = service.getTimeLine(userId, date)

            assertThat(result.items).hasSize(1)
            val item = result.items.first() as TimeLineItemDTO.Single
            assertThat(item.mealFoodName).isEqualTo("된장찌개")
            assertThat(item.grade).isEqualTo(JudgmentGrade.RECOMMEND)
            assertThat(item.etcCount).isEqualTo(0)
        }

        @Test
        fun `음식이 2개 이상인 식사 기록은 Group으로 매핑된다`() {
            val mealRecord = MealRecordFixture.mealRecord()
            val food1 = MealRecordFixture.mealFood(id = 1L, foodId = 1L)
            val food2 = MealRecordFixture.mealFood(id = 2L, foodId = 2L)
            val food3 = MealRecordFixture.mealFood(id = 3L, foodId = 3L)

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(mealRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(emptyList())
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(food1, food2, food3))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(
                FoodFixture.food(id = 1L, name = "된장찌개"),
                FoodFixture.food(id = 2L, name = "김치"),
                FoodFixture.food(id = 3L, name = "밥"),
            ))

            val result = service.getTimeLine(userId, date)

            val item = result.items.first() as TimeLineItemDTO.Group
            assertThat(item.representativeFoods).containsExactly("된장찌개", "김치")
            assertThat(item.etcCount).isEqualTo(1)
        }

        @Test
        fun `판정 등급이 없는 음식은 UNKNOWN으로 매핑된다`() {
            val mealRecord = MealRecordFixture.mealRecord()
            val mealFood = MealRecordFixture.mealFood(judgedGrade = null)
            val food = FoodFixture.food(id = 1L)

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(mealRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(emptyList())
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(mealFood))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(food))

            val result = service.getTimeLine(userId, date)

            val item = result.items.first() as TimeLineItemDTO.Single
            assertThat(item.grade).isEqualTo(JudgmentGrade.UNKNOWN)
        }

        @Test
        fun `Single 식사에 연결된 증상은 standalone Symptom 아이템으로 표시된다`() {
            val mealRecord = MealRecordFixture.mealRecord(eatenAt = LocalDateTime.of(2026, 6, 17, 12, 0, 0))
            val symptom = SymptomFixture.symptom(
                mealRecordId = MealRecordFixture.MEAL_RECORD_ID,
                occurredAt = LocalDateTime.of(2026, 6, 17, 12, 30, 0),
                symptomState = SymptomState.UNCOMFORTABLE,
            )
            val mealFood = MealRecordFixture.mealFood()
            val food = FoodFixture.food(id = 1L)

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(mealRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(listOf(symptom))
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(mealFood))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(food))

            val result = service.getTimeLine(userId, date)

            val symptomItem = result.items.filterIsInstance<TimeLineItemDTO.Symptom>().first()
            assertThat(symptomItem.symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
            assertThat(symptomItem.afterMealMinutes).isEqualTo(30)
        }

        @Test
        fun `Group 식사에 연결된 증상은 connectedSymptoms에 임베드되고 standalone 아이템에서 제외된다`() {
            val mealRecord = MealRecordFixture.mealRecord(eatenAt = LocalDateTime.of(2026, 6, 17, 12, 0, 0))
            val food1 = MealRecordFixture.mealFood(id = 1L, foodId = 1L)
            val food2 = MealRecordFixture.mealFood(id = 2L, foodId = 2L)
            val symptom = SymptomFixture.symptom(
                mealRecordId = MealRecordFixture.MEAL_RECORD_ID,
                occurredAt = LocalDateTime.of(2026, 6, 17, 13, 10, 0),
                symptomState = SymptomState.UNCOMFORTABLE,
                symptomTypes = setOf(SymptomType.ACID_REFLUX, SymptomType.CHEST_TIGHTNESS),
            )

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(mealRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(listOf(symptom))
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(food1, food2))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(
                FoodFixture.food(id = 1L, name = "된장찌개"),
                FoodFixture.food(id = 2L, name = "밥"),
            ))

            val result = service.getTimeLine(userId, date)

            assertThat(result.items.filterIsInstance<TimeLineItemDTO.Symptom>()).isEmpty()
            val groupItem = result.items.first() as TimeLineItemDTO.Group
            val connected = groupItem.connectedSymptoms!!
            assertThat(connected.symptomId).isEqualTo(SymptomFixture.SYMPTOM_EXTERNAL_ID.toString())
            assertThat(connected.symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
            assertThat(connected.afterMealMinutes).isEqualTo(70)
            assertThat(connected.representativeSymptoms).hasSize(2)
            assertThat(connected.etcCount).isEqualTo(0)
        }

        @Test
        fun `Group에 연결된 증상이 여러 개이면 가장 최근 증상 기준으로 connectedSymptoms가 설정된다`() {
            val mealRecord = MealRecordFixture.mealRecord(eatenAt = LocalDateTime.of(2026, 6, 17, 12, 0, 0))
            val food1 = MealRecordFixture.mealFood(id = 1L, foodId = 1L)
            val food2 = MealRecordFixture.mealFood(id = 2L, foodId = 2L)
            val olderSymptom = SymptomFixture.symptom(
                id = 1L,
                mealRecordId = MealRecordFixture.MEAL_RECORD_ID,
                occurredAt = LocalDateTime.of(2026, 6, 17, 12, 30, 0),
                symptomState = SymptomState.COMFORTABLE,
                symptomTypes = setOf(SymptomType.COUGH),
            )
            val recentSymptom = SymptomFixture.symptom(
                id = 2L,
                mealRecordId = MealRecordFixture.MEAL_RECORD_ID,
                occurredAt = LocalDateTime.of(2026, 6, 17, 13, 0, 0),
                symptomState = SymptomState.UNCOMFORTABLE,
                symptomTypes = setOf(SymptomType.ACID_REFLUX),
            )

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(mealRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(listOf(olderSymptom, recentSymptom))
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(food1, food2))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(
                FoodFixture.food(id = 1L), FoodFixture.food(id = 2L),
            ))

            val result = service.getTimeLine(userId, date)

            val groupItem = result.items.first() as TimeLineItemDTO.Group
            val connected = groupItem.connectedSymptoms!!
            assertThat(connected.symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
            assertThat(connected.afterMealMinutes).isEqualTo(60)
            assertThat(connected.representativeSymptoms).containsExactlyInAnyOrder(SymptomType.COUGH, SymptomType.ACID_REFLUX)
        }

        @Test
        fun `Group에 연결된 증상 유형이 3개 이상이면 representativeSymptoms는 최대 2개이고 etcCount에 나머지 수가 담긴다`() {
            val mealRecord = MealRecordFixture.mealRecord(eatenAt = LocalDateTime.of(2026, 6, 17, 12, 0, 0))
            val food1 = MealRecordFixture.mealFood(id = 1L, foodId = 1L)
            val food2 = MealRecordFixture.mealFood(id = 2L, foodId = 2L)
            val symptom = SymptomFixture.symptom(
                mealRecordId = MealRecordFixture.MEAL_RECORD_ID,
                occurredAt = LocalDateTime.of(2026, 6, 17, 12, 30, 0),
                symptomTypes = setOf(
                    SymptomType.ACID_REFLUX,
                    SymptomType.CHEST_TIGHTNESS,
                    SymptomType.COUGH,
                ),
            )

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(mealRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(listOf(symptom))
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(food1, food2))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(
                FoodFixture.food(id = 1L), FoodFixture.food(id = 2L),
            ))

            val result = service.getTimeLine(userId, date)

            val groupItem = result.items.first() as TimeLineItemDTO.Group
            val connected = groupItem.connectedSymptoms!!
            assertThat(connected.representativeSymptoms).hasSize(2)
            assertThat(connected.etcCount).isEqualTo(1)
        }

        @Test
        fun `Group에 연결된 증상이 없으면 connectedSymptoms는 null이다`() {
            val mealRecord = MealRecordFixture.mealRecord()
            val food1 = MealRecordFixture.mealFood(id = 1L, foodId = 1L)
            val food2 = MealRecordFixture.mealFood(id = 2L, foodId = 2L)

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(mealRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(emptyList())
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(food1, food2))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(
                FoodFixture.food(id = 1L), FoodFixture.food(id = 2L),
            ))

            val result = service.getTimeLine(userId, date)

            val groupItem = result.items.first() as TimeLineItemDTO.Group
            assertThat(groupItem.connectedSymptoms).isNull()
        }

        @Test
        fun `미연결 증상은 standalone Symptom 아이템으로 표시되며 afterMealMinutes는 0이다`() {
            val symptom = SymptomFixture.symptom(
                mealRecordId = null,
                occurredAt = LocalDateTime.of(2026, 6, 17, 14, 32, 0),
                symptomState = SymptomState.UNCOMFORTABLE,
            )

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(emptyList())
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(listOf(symptom))

            val result = service.getTimeLine(userId, date)

            assertThat(result.items).hasSize(1)
            val symptomItem = result.items.first() as TimeLineItemDTO.Symptom
            assertThat(symptomItem.symptomState).isEqualTo(SymptomState.UNCOMFORTABLE)
            assertThat(symptomItem.afterMealMinutes).isEqualTo(0)
        }

        @Test
        fun `아이템들은 시간 오름차순으로 정렬된다`() {
            val earlyRecord = MealRecordFixture.mealRecord(id = 1L, eatenAt = LocalDateTime.of(2026, 6, 17, 8, 0, 0))
            val lateRecord = MealRecordFixture.mealRecord(id = 2L, eatenAt = LocalDateTime.of(2026, 6, 17, 18, 0, 0))
            val midSymptom = SymptomFixture.symptom(
                mealRecordId = 1L,
                occurredAt = LocalDateTime.of(2026, 6, 17, 12, 0, 0),
            )
            val food = MealRecordFixture.mealFood(mealRecordId = 1L)
            val food2 = MealRecordFixture.mealFood(id = 2L, mealRecordId = 2L)

            whenever(mealRecordRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(lateRecord, earlyRecord))
            whenever(symptomRepository.findByUser_IdAndOccurredAtBetween(any(), any(), any())).thenReturn(listOf(midSymptom))
            whenever(mealFoodRepository.findByMealRecordIdInOrderByMealRecordIdAscEatenAtAsc(any())).thenReturn(listOf(food, food2))
            whenever(foodRepository.findAllByIdsIncludingDeleted(any())).thenReturn(listOf(FoodFixture.food()))

            val result = service.getTimeLine(userId, date)

            // Single(8:00) → Symptom(12:00, Single 연결) → Single(18:00)
            assertThat(result.items[0]).isInstanceOf(TimeLineItemDTO.Single::class.java)
            assertThat(result.items[1]).isInstanceOf(TimeLineItemDTO.Symptom::class.java)
            assertThat(result.items[2]).isInstanceOf(TimeLineItemDTO.Single::class.java)
        }
    }

    @Nested
    inner class `주간 판정 등급 조회` {

        @Test
        fun `항상 7개의 날짜를 반환한다`() {
            whenever(mealFoodRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(emptyList())

            val result = service.getWeeklyJudgements(userId, date)

            assertThat(result).hasSize(7)
        }

        @Test
        fun `화요일 기준으로 해당 주 일요일부터 토요일까지 반환한다`() {
            val tuesday = LocalDate.of(2026, 6, 16)
            whenever(mealFoodRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(emptyList())

            val result = service.getWeeklyJudgements(userId, tuesday)

            assertThat(result.first().date.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
            assertThat(result.last().date.dayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
            assertThat(result.first().date).isEqualTo(LocalDate.of(2026, 6, 14))
        }

        @Test
        fun `식사 기록이 없는 날은 빈 judgementList를 반환한다`() {
            whenever(mealFoodRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(emptyList())

            val result = service.getWeeklyJudgements(userId, date)

            assertThat(result).allMatch { it.judgementList.isEmpty() }
        }

        @Test
        fun `날짜별 판정 등급이 올바르게 매핑된다`() {
            val wednesday = LocalDate.of(2026, 6, 17)
            val food1 = MealRecordFixture.mealFood(id = 1L, eatenAt = wednesday.atTime(8, 0), judgedGrade = JudgmentGrade.RECOMMEND)
            val food2 = MealRecordFixture.mealFood(id = 2L, eatenAt = wednesday.atTime(12, 0), judgedGrade = JudgmentGrade.RISK)

            whenever(mealFoodRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(food1, food2))

            val result = service.getWeeklyJudgements(userId, wednesday)

            val wednesdayResult = result.find { it.date == wednesday }!!
            assertThat(wednesdayResult.judgementList).containsExactlyInAnyOrder(JudgmentGrade.RECOMMEND, JudgmentGrade.RISK)
        }

        @Test
        fun `판정 등급이 null인 음식은 제외된다`() {
            val wednesday = LocalDate.of(2026, 6, 17)
            val graded = MealRecordFixture.mealFood(id = 1L, eatenAt = wednesday.atTime(8, 0), judgedGrade = JudgmentGrade.CAUTION)
            val ungraded = MealRecordFixture.mealFood(id = 2L, eatenAt = wednesday.atTime(12, 0), judgedGrade = null)

            whenever(mealFoodRepository.findByUser_IdAndEatenAtBetween(any(), any(), any())).thenReturn(listOf(graded, ungraded))

            val result = service.getWeeklyJudgements(userId, wednesday)

            val wednesdayResult = result.find { it.date == wednesday }!!
            assertThat(wednesdayResult.judgementList).containsExactly(JudgmentGrade.CAUTION)
        }
    }
}
