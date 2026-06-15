package com.gerd.domain.meal.service

import com.gerd.domain.meal.dto.MealGroupDTO
import com.gerd.domain.meal.exception.MealErrorCode
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.MealRecordFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class MealRecordQueryServiceTest {

    @Mock
    private lateinit var mealRecordRepository: MealRecordRepository

    @Mock
    private lateinit var mealRecordAssembler: MealRecordAssembler

    private lateinit var service: MealRecordQueryService

    private val userId = 1L
    private val mealId = MealRecordFixture.MEAL_EXTERNAL_ID

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        service = MealRecordQueryService(mealRecordRepository, mealRecordAssembler)
    }

    @Nested
    inner class `단건 조회` {

        @Test
        fun `형식이 잘못된 mealId면 MEAL_NOT_FOUND`() {
            whenever(mealRecordAssembler.parseUuid("bad")).thenReturn(null)

            assertThatThrownBy { service.getDetail("bad", userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_NOT_FOUND)
        }

        @Test
        fun `기록이 없거나 타인 소유면 MEAL_NOT_FOUND`() {
            whenever(mealRecordAssembler.parseUuid(mealId.toString())).thenReturn(mealId)
            whenever(mealRecordRepository.findByExternalIdAndUserId(mealId, userId)).thenReturn(null)

            assertThatThrownBy { service.getDetail(mealId.toString(), userId) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(MealErrorCode.MEAL_NOT_FOUND)
        }

        @Test
        fun `본인 기록이면 상세를 반환한다`() {
            val record = MealRecordFixture.mealRecord(userId = userId)
            whenever(mealRecordAssembler.parseUuid(mealId.toString())).thenReturn(mealId)
            whenever(mealRecordRepository.findByExternalIdAndUserId(mealId, userId)).thenReturn(record)
            whenever(mealRecordAssembler.toDetail(record)).thenReturn(detailStub())

            val result = service.getDetail(mealId.toString(), userId)

            assertThat(result.mealId).isEqualTo(mealId.toString())
        }
    }

    @Nested
    inner class `날짜별 조회` {

        @Test
        fun `날짜 경계로 조회해 끼니 단위로 그룹핑한다`() {
            val day = LocalDate.of(2026, 6, 11)
            val from = LocalDateTime.of(2026, 6, 11, 0, 0)
            val to = LocalDateTime.of(2026, 6, 12, 0, 0)
            val records = listOf(MealRecordFixture.mealRecord(userId = userId))
            val groups = listOf(groupStub())
            whenever(mealRecordAssembler.parseDate("2026-06-11")).thenReturn(day)
            whenever(mealRecordAssembler.toDayRange(day)).thenReturn(from to to)
            whenever(mealRecordRepository.findDailyRecords(userId, from, to)).thenReturn(records)
            whenever(mealRecordAssembler.toSummaries(records)).thenReturn(emptyList())
            whenever(mealRecordAssembler.toGroups(any())).thenReturn(groups)

            val result = service.getDaily("2026-06-11", userId)

            assertThat(result).isEqualTo(groups)
        }

        @Test
        fun `date 미전달이면 오늘 범위를 사용한다`() {
            val today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
            val from = today.atStartOfDay()
            val to = today.plusDays(1).atStartOfDay()
            whenever(mealRecordAssembler.parseDate(null)).thenReturn(today)
            whenever(mealRecordAssembler.toDayRange(today)).thenReturn(from to to)
            whenever(mealRecordRepository.findDailyRecords(eq(userId), eq(from), eq(to))).thenReturn(emptyList())
            whenever(mealRecordAssembler.toSummaries(emptyList())).thenReturn(emptyList())
            whenever(mealRecordAssembler.toGroups(emptyList())).thenReturn(emptyList())

            val result = service.getDaily(null, userId)

            assertThat(result).isEmpty()
        }
    }

    private fun groupStub() = MealGroupDTO(
        mealGroupId = MealRecordFixture.MEAL_GROUP_ID.toString(),
        eatenAt = "2026-06-11T12:30:00+09:00",
        records = emptyList(),
    )

    private fun detailStub() = com.gerd.domain.meal.dto.MealRecordDetailDTO(
        mealId = mealId.toString(),
        mealGroupId = MealRecordFixture.MEAL_GROUP_ID.toString(),
        eatenAt = "2026-06-11T12:30:00+09:00",
        memo = null,
        judgedGrade = null,
        food = com.gerd.domain.meal.dto.MealRecordDetailDTO.MealFoodDetailDTO("x", "음식", "cat", null),
        stateRecords = emptyList(),
    )
}
