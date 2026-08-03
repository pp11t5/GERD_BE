package com.gerd.domain.notification.repository

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.notification.entity.NotificationPending
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.CANCELLED
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.PENDING
import com.gerd.domain.notification.entity.enums.NotificationPendingStatus.SENT
import com.gerd.domain.notification.entity.enums.NotificationType
import com.gerd.domain.notification.entity.enums.NotificationType.DAILY_RECORD
import com.gerd.domain.notification.entity.enums.NotificationType.POST_MEAL
import com.gerd.global.config.JpaAuditingConfig
import com.gerd.global.config.QuerydslTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class, JpaAuditingConfig::class)
class NotificationPendingRepositoryTest @Autowired constructor(
    private val notificationPendingRepository: NotificationPendingRepository,
    private val userRepository: UserRepository,
) {

    @Nested
    inner class `식후 알림 쿨다운 이력 조회` {

        @Test
        fun `CANCELLED 알림만 있으면 쿨다운 이력이 없다고 판단한다`() {
            val user = saveUser()
            val pending = savePending(user = user, status = CANCELLED)

            val exists = existsCooldown(user, pending.createdAt!!.minusSeconds(1))

            assertThat(exists).isFalse()
        }

        @ParameterizedTest
        @EnumSource(
            value = NotificationPendingStatus::class,
            names = ["PENDING", "SENT"],
        )
        fun `PENDING 또는 SENT 알림이 있으면 쿨다운 이력이 있다고 판단한다`(
            status: NotificationPendingStatus,
        ) {
            val user = saveUser()
            val pending = savePending(user = user, status = status)

            val exists = existsCooldown(user, pending.createdAt!!.minusSeconds(1))

            assertThat(exists).isTrue()
        }

        @Test
        fun `다른 사용자의 알림은 쿨다운 이력에서 제외한다`() {
            val user = saveUser()
            val otherUser = saveUser("other@test.com")
            val pending = savePending(user = otherUser)

            val exists = existsCooldown(user, pending.createdAt!!.minusSeconds(1))

            assertThat(exists).isFalse()
        }

        @Test
        fun `다른 타입의 알림은 쿨다운 이력에서 제외한다`() {
            val user = saveUser()
            val pending = savePending(user = user, type = DAILY_RECORD)

            val exists = existsCooldown(user, pending.createdAt!!.minusSeconds(1))

            assertThat(exists).isFalse()
        }

        @Test
        fun `이연 알림은 쿨다운 이력에서 제외한다`() {
            val user = saveUser()
            val pending = savePending(user = user, delayed = true)

            val exists = existsCooldown(user, pending.createdAt!!.minusSeconds(1))

            assertThat(exists).isFalse()
        }

        @Test
        fun `기준 시각 이전에 생성된 알림은 쿨다운 이력에서 제외한다`() {
            val user = saveUser()
            val pending = savePending(user = user)

            val exists = existsCooldown(user, pending.createdAt!!.plusNanos(1))

            assertThat(exists).isFalse()
        }
    }

    private fun existsCooldown(user: User, createdAt: LocalDateTime): Boolean =
        notificationPendingRepository.existsByUserIdAndTypeAndDelayedFalseAndStatusInAndCreatedAtAfter(
            userId = user.id!!,
            type = POST_MEAL,
            statuses = setOf(PENDING, SENT),
            createdAt = createdAt,
        )

    private fun saveUser(email: String = "user@test.com"): User =
        userRepository.save(User(email = email, nickname = email.substringBefore("@")))

    private fun savePending(
        user: User,
        type: NotificationType = POST_MEAL,
        status: NotificationPendingStatus = PENDING,
        delayed: Boolean = false,
    ): NotificationPending =
        notificationPendingRepository.saveAndFlush(
            NotificationPending(
                user = user,
                type = type,
                scheduledAt = LocalDateTime.now().plusHours(2),
                delayed = delayed,
                status = status,
            ),
        )
}
