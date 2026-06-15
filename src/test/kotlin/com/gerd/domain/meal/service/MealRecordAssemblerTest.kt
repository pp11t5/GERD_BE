package com.gerd.domain.meal.service

import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.service.FoodCategoryReader
import com.gerd.domain.meal.dto.MealRecordSummaryDTO
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import com.gerd.global.fixture.MealRecordFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MealRecordAssemblerTest {

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var foodCategoryReader: FoodCategoryReader

    private lateinit var assembler: MealRecordAssembler

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        assembler = MealRecordAssembler(foodRepository, foodCategoryReader)
    }

    @Nested
    inner class `parseEatenAt` {

        @Test
        fun `미전달이면 현재 시각으로 채운다`() {
            val before = LocalDateTime.now()

            val result = assembler.parseEatenAt(null)

            assertThat(result).isAfterOrEqualTo(before.minusSeconds(1))
        }

        @Test
        fun `offset 포함 ISO-8601을 KST LocalDateTime으로 변환한다`() {
            // UTC 03:30 == KST 12:30
            val result = assembler.parseEatenAt("2026-06-11T03:30:00+00:00")

            assertThat(result).isEqualTo(LocalDateTime.of(2026, 6, 11, 12, 30, 0))
        }

        @Test
        fun `형식이 올바르지 않으면 INVALID_DATE_TIME`() {
            assertThatThrownBy { assembler.parseEatenAt("2026-06-11 12:30") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.INVALID_DATE_TIME)
        }
    }

    @Nested
    inner class `parseDate` {

        @Test
        fun `미전달이면 오늘로 채운다`() {
            assertThat(assembler.parseDate(null)).isEqualTo(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")))
        }

        @Test
        fun `YYYY-MM-DD를 파싱한다`() {
            assertThat(assembler.parseDate("2026-06-11")).isEqualTo(LocalDate.of(2026, 6, 11))
        }

        @Test
        fun `형식이 올바르지 않으면 INVALID_DATE_TIME`() {
            assertThatThrownBy { assembler.parseDate("2026/06/11") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.INVALID_DATE_TIME)
        }
    }

    @Nested
    inner class `parseUuid` {

        @Test
        fun `형식이 잘못된 UUID는 null을 반환한다`() {
            assertThat(assembler.parseUuid("not-a-uuid")).isNull()
        }

        @Test
        fun `유효한 UUID를 파싱한다`() {
            val id = UUID.randomUUID()
            assertThat(assembler.parseUuid(id.toString())).isEqualTo(id)
        }
    }

    @Nested
    inner class `toDayRange` {

        @Test
        fun `해당 날짜 00시부터 다음날 00시 직전까지의 경계를 만든다`() {
            val (from, to) = assembler.toDayRange(LocalDate.of(2026, 6, 11))

            assertThat(from).isEqualTo(LocalDateTime.of(2026, 6, 11, 0, 0, 0))
            assertThat(to).isEqualTo(LocalDateTime.of(2026, 6, 12, 0, 0, 0))
        }
    }

    @Nested
    inner class `toSummaries` {

        @Test
        fun `삭제된 음식도 포함해 음식 정보를 채운다`() {
            val record = MealRecordFixture.mealRecord(id = 1L, foodId = 10L)
            val food = FoodFixture.food(id = 10L, name = "된장찌개")
            whenever(foodRepository.findAllByIdsIncludingDeleted(listOf(10L))).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(10L))).thenReturn(mapOf(10L to "soup_stew"))

            val result = assembler.toSummaries(listOf(record))

            assertThat(result).hasSize(1)
            assertThat(result[0].food.name).isEqualTo("된장찌개")
            assertThat(result[0].food.category).isEqualTo("soup_stew")
            assertThat(result[0].eatenAt).isEqualTo("2026-06-11T12:30:00+09:00")
        }

        @Test
        fun `빈 입력이면 빈 리스트를 반환한다`() {
            assertThat(assembler.toSummaries(emptyList())).isEmpty()
        }
    }

    @Nested
    inner class `toGroups` {

        @Test
        fun `mealGroupId로 묶고 대표 시각은 첫 기록 시각이다`() {
            val groupA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000000")
            val groupB = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000000")
            // 입력은 eatenAt asc 가정 (repository가 정렬해 전달)
            val summaries = listOf(
                summary(groupA, "2026-06-11T09:02:00+09:00"),
                summary(groupB, "2026-06-11T18:48:00+09:00"),
                summary(groupB, "2026-06-11T18:55:00+09:00"),
            )

            val groups = assembler.toGroups(summaries)

            assertThat(groups.map { it.mealGroupId }).containsExactly(groupA.toString(), groupB.toString())
            assertThat(groups[0].eatenAt).isEqualTo("2026-06-11T09:02:00+09:00")
            assertThat(groups[1].eatenAt).isEqualTo("2026-06-11T18:48:00+09:00") // 소속 records 최솟값
            assertThat(groups[1].records).hasSize(2)
        }

        @Test
        fun `기록이 없으면 빈 리스트를 반환한다`() {
            assertThat(assembler.toGroups(emptyList())).isEmpty()
        }
    }

    @Nested
    inner class `toDetail` {

        @Test
        fun `음식 설명과 상태 기록 빈 배열을 포함한다`() {
            val record = MealRecordFixture.mealRecord(id = 1L, foodId = 10L, memo = "메모")
            val food = FoodFixture.food(id = 10L, name = "된장찌개").apply { description = "저자극 한식" }
            whenever(foodRepository.findAllByIdsIncludingDeleted(listOf(10L))).thenReturn(listOf(food))
            whenever(foodCategoryReader.loadPrimaryByFoodIds(listOf(10L))).thenReturn(mapOf(10L to "soup_stew"))

            val result = assembler.toDetail(record)

            assertThat(result.memo).isEqualTo("메모")
            assertThat(result.food.description).isEqualTo("저자극 한식")
            assertThat(result.stateRecords).isEmpty()
        }
    }

    private fun summary(groupId: UUID, eatenAt: String) =
        MealRecordSummaryDTO(
            mealId = UUID.randomUUID().toString(),
            mealGroupId = groupId.toString(),
            eatenAt = eatenAt,
            food = com.gerd.domain.food.dto.FoodSummaryDTO("x", "음식", "cat"),
            judgedGrade = null,
        )
}
