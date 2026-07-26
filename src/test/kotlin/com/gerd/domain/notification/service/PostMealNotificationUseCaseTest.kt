package com.gerd.domain.notification.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.PENDING
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.repository.NotificationPendingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
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
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PostMealNotificationUseCaseTest {

    @Mock private lateinit var notificationPendingRepository: NotificationPendingRepository
    @Mock private lateinit var postMealPendingSender: PostMealPendingSender
    @Mock private lateinit var userRepository: UserRepository

    @InjectMocks private lateinit var useCase: PostMealNotificationUseCase

    private val userId = 1L
    private val mealRecordId = 100L

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
            whenever(
                notificationPendingRepository.existsByUserIdAndTypeAndDelayedFalseAndCreatedAtAfter(
                    eq(userId), eq(NotificationType.POST_MEAL), any(),
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
        fun `낮 시간대에 90분 내 이력이 있으면 저장하지 않는다`() {
            val now = LocalDateTime.of(2026, 7, 6, 12, 0)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mock<User>()))
            whenever(
                notificationPendingRepository.existsByUserIdAndTypeAndDelayedFalseAndCreatedAtAfter(
                    eq(userId), eq(NotificationType.POST_MEAL), any(),
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

            withFixedNow(now) { useCase.enqueue(userId, mealRecordId) }

            val captor = argumentCaptor<NotificationPending>()
            verify(notificationPendingRepository).save(captor.capture())
            with(captor.firstValue) {
                assertThat(delayed).isTrue()
                assertThat(scheduledAt).isEqualTo(LocalDateTime.of(2026, 7, 7, 9, 0))
            }
            // delayed 분기라 쿨다운 조회 자체가 일어나지 않는다
            verify(notificationPendingRepository, never())
                .existsByUserIdAndTypeAndDelayedFalseAndCreatedAtAfter(any(), any(), any())
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
