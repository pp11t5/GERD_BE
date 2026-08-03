package com.gerd.domain.notification.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.meal.repository.MealRecordRepository
import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.PENDING
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.SENT
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.repository.NotificationPendingRepository
import com.gerd.global.config.properties.NotificationProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostMealNotificationUseCaseTest {

    @Mock private lateinit var notificationPendingRepository: NotificationPendingRepository
    @Mock private lateinit var postMealPendingSender: PostMealPendingSender
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var mealRecordRepository: MealRecordRepository

    private lateinit var useCase: PostMealNotificationUseCase

    private val userId = 1L
    private val mealRecordId = 100L

    @BeforeEach
    fun setUp() {
        useCase = createUseCase(NotificationProperties())
    }

    private fun createUseCase(properties: NotificationProperties) = PostMealNotificationUseCase(
        notificationPendingRepository,
        postMealPendingSender,
        userRepository,
        mealRecordRepository,
        properties,
    )

    // now()를 고정해 낮/야간 분기를 결정적으로 테스트 — CALLS_REAL_METHODS로 of()/atTime() 등은 실제 동작 유지
    private inline fun withFixedNow(now: LocalDateTime, block: () -> Unit) {
        Mockito.mockStatic(LocalDateTime::class.java, Mockito.CALLS_REAL_METHODS).use { mocked ->
            mocked.`when`<LocalDateTime> { LocalDateTime.now() }.thenReturn(now)
            block()
        }
    }

    @Nested
    inner class `enqueue` {

        @Test
        fun `낮 시간대이고 쿨다운 이력이 없으면 delayed=false로 저장한다`() {
            // 12:00 + 2h = 14:00 → 야간 아님
            val now = LocalDateTime.of(2026, 7, 6, 12, 0)
            val user = mock<User>()
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(mealRecordRepository.existsById(mealRecordId)).thenReturn(true)
            whenever(
                notificationPendingRepository.existsByUserIdAndTypeAndDelayedFalseAndStatusInAndCreatedAtAfter(
                    eq(userId), eq(NotificationType.POST_MEAL), eq(setOf(PENDING, SENT)), any(),
                )
            ).thenReturn(false)

            withFixedNow(now) { useCase.enqueue(userId, mealRecordId) }

            val captor = argumentCaptor<NotificationPending>()
            verify(notificationPendingRepository).save(captor.capture())
            with(captor.firstValue) {
                assertThat(delayed).isFalse()
                assertThat(scheduledAt).isEqualTo(LocalDateTime.of(2026, 7, 6, 14, 0))
                assertThat(type).isEqualTo(NotificationType.POST_MEAL)
                assertThat(this.mealRecordId).isEqualTo(mealRecordId)
                assertThat(user).isSameAs(this.user)
            }
        }

        @Test
        fun `낮 시간대에 90분 내 PENDING 또는 SENT 이력이 있으면 저장하지 않는다`() {
            val now = LocalDateTime.of(2026, 7, 6, 12, 0)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mock<User>()))
            whenever(mealRecordRepository.existsById(mealRecordId)).thenReturn(true)
            whenever(
                notificationPendingRepository.existsByUserIdAndTypeAndDelayedFalseAndStatusInAndCreatedAtAfter(
                    eq(userId), eq(NotificationType.POST_MEAL), eq(setOf(PENDING, SENT)), any(),
                )
            ).thenReturn(true)

            withFixedNow(now) { useCase.enqueue(userId, mealRecordId) }

            verify(notificationPendingRepository, never()).save(any())
        }

        @Test
        fun `야간이면 쿨다운을 확인하지 않고 다음 09시로 이연 저장한다`() {
            // 21:00 + 2h = 23:00 → 야간 → 다음날 09:00 이연
            val now = LocalDateTime.of(2026, 7, 6, 21, 0)
            val user = mock<User>()
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(mealRecordRepository.existsById(mealRecordId)).thenReturn(true)

            withFixedNow(now) { useCase.enqueue(userId, mealRecordId) }

            val captor = argumentCaptor<NotificationPending>()
            verify(notificationPendingRepository).save(captor.capture())
            with(captor.firstValue) {
                assertThat(delayed).isTrue()
                assertThat(scheduledAt).isEqualTo(LocalDateTime.of(2026, 7, 7, 9, 0))
            }
            // delayed 분기라 쿨다운 조회 자체가 일어나지 않는다
            verify(notificationPendingRepository, never())
                .existsByUserIdAndTypeAndDelayedFalseAndStatusInAndCreatedAtAfter(any(), any(), any(), any())
        }

        @Test
        fun `staging 설정에서는 식후 1분 시각이 20시 40분 이후면 당일 21시로 이연한다`() {
            useCase = createUseCase(
                NotificationProperties(
                    delay = Duration.ofMinutes(1),
                    quietHoursStart = LocalTime.of(20, 40),
                    deferredTime = LocalTime.of(21, 0),
                ),
            )
            val now = LocalDateTime.of(2026, 8, 3, 20, 39)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mock<User>()))
            whenever(mealRecordRepository.existsById(mealRecordId)).thenReturn(true)

            withFixedNow(now) { useCase.enqueue(userId, mealRecordId) }

            val captor = argumentCaptor<NotificationPending>()
            verify(notificationPendingRepository).save(captor.capture())
            with(captor.firstValue) {
                assertThat(delayed).isTrue()
                assertThat(scheduledAt).isEqualTo(LocalDateTime.of(2026, 8, 3, 21, 0))
            }
        }

        @Test
        fun `유저가 존재하지 않으면 예약을 스킵한다`() {
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            useCase.enqueue(userId, mealRecordId)

            verify(notificationPendingRepository, never())
                .existsByUserIdAndTypeAndDelayedFalseAndStatusInAndCreatedAtAfter(any(), any(), any(), any())
            verify(notificationPendingRepository, never()).save(any())
        }

        @Test
        fun `AFTER_COMMIT 비동기 처리 사이 끼니가 삭제됐으면 예약을 스킵한다`() {
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mock<User>()))
            whenever(mealRecordRepository.existsById(mealRecordId)).thenReturn(false)

            useCase.enqueue(userId, mealRecordId)

            verify(notificationPendingRepository, never())
                .existsByUserIdAndTypeAndDelayedFalseAndStatusInAndCreatedAtAfter(any(), any(), any(), any())
            verify(notificationPendingRepository, never()).save(any())
        }
    }

    @Nested
    inner class `cancelByMealRecordId` {

        @Test
        fun `해당 식사의 PENDING을 조회해 각각 취소한다`() {
            val first = mock<NotificationPending>()
            val second = mock<NotificationPending>()
            whenever(
                notificationPendingRepository.findByMealRecordIdAndUserIdAndStatus(mealRecordId, userId, PENDING)
            ).thenReturn(listOf(first, second))

            useCase.cancelByMealRecordId(userId, mealRecordId)

            verify(first).cancel()
            verify(second).cancel()
        }

        @Test
        fun `취소 대상이 없으면 아무것도 하지 않는다`() {
            whenever(
                notificationPendingRepository.findByMealRecordIdAndUserIdAndStatus(mealRecordId, userId, PENDING)
            ).thenReturn(emptyList())

            useCase.cancelByMealRecordId(userId, mealRecordId)

            verify(notificationPendingRepository)
                .findByMealRecordIdAndUserIdAndStatus(mealRecordId, userId, PENDING)
        }
    }
}
