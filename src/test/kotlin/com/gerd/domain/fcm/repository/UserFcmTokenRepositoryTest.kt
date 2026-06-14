package com.gerd.domain.fcm.repository

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.global.config.QuerydslTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class UserFcmTokenRepositoryTest @Autowired constructor(
    private val userFcmTokenRepository: UserFcmTokenRepository,
    private val userRepository: UserRepository,
    private val userNotificationSettingRepository: UserNotificationSettingRepository,
) {

    private lateinit var user: User
    private lateinit var savedToken: UserFcmToken

    @BeforeEach
    fun setUp() {
        user = userRepository.save(User(email = "user@test.com", role = UserRole.USER))
        savedToken = userFcmTokenRepository.save(
            UserFcmToken(user = user, platform = DevicePlatform.IOS, token = "fcm-token-abc")
        )
    }

    // 매일 기록 알림 대상 유저(시간대·활성·미탈퇴·토큰보유) 생성
    private fun saveDailyUser(
        email: String,
        time: DailyNotificationTime = DailyNotificationTime.NIGHT_9,
        enabled: Boolean = true,
        withToken: Boolean = true,
        withdrawn: Boolean = false,
    ): User {
        val saved = userRepository.save(
            User(email = email, role = UserRole.USER).also { if (withdrawn) it.withdraw() }
        )
        userNotificationSettingRepository.save(
            UserNotificationSetting(
                user = saved,
                dailyRecordNotificationEnabled = enabled,
                dailyNotificationTime = time,
            )
        )
        if (withToken) {
            userFcmTokenRepository.save(
                UserFcmToken(user = saved, platform = DevicePlatform.IOS, token = "token-$email")
            )
        }
        return saved
    }

    @Nested
    inner class `findByToken` {

        @Test
        fun `토큰 값으로 FCM 토큰을 조회한다`() {
            val result = userFcmTokenRepository.findByToken("fcm-token-abc")

            assertThat(result).isNotNull
            assertThat(result!!.token).isEqualTo("fcm-token-abc")
            assertThat(result.platform).isEqualTo(DevicePlatform.IOS)
        }

        @Test
        fun `존재하지 않는 토큰이면 null을 반환한다`() {
            val result = userFcmTokenRepository.findByToken("not-exist-token")

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class `findByDailyRecordNotificationEnabledAndDailyNotificationTime` {

        @Test
        fun `해당 시간대의 활성·미탈퇴·토큰보유 유저 토큰만 조회한다`() {
            saveDailyUser("target@test.com")                                       // 포함
            saveDailyUser("other-time@test.com", time = DailyNotificationTime.MORNING_8) // 제외: 다른 시간대
            saveDailyUser("disabled@test.com", enabled = false)                    // 제외: 비활성
            saveDailyUser("withdrawn@test.com", withdrawn = true)                  // 제외: 탈퇴 유예
            saveDailyUser("no-token@test.com", withToken = false)                  // 제외: 토큰 없음

            val slice = userFcmTokenRepository
                .findByDailyRecordNotificationEnabledAndDailyNotificationTime(
                    DailyNotificationTime.NIGHT_9, 0L, PageRequest.of(0, 10),
                )

            assertThat(slice.content.map { it.token }).containsExactly("token-target@test.com")
        }

        @Test
        fun `userId 커서로 다음 페이지를 이어 조회하고 마지막 페이지에서 hasNext는 false다`() {
            val u1 = saveDailyUser("c1@test.com")
            val u2 = saveDailyUser("c2@test.com")
            val u3 = saveDailyUser("c3@test.com")

            // 1페이지 — 2건, 다음 있음, userId ASC
            val page1 = userFcmTokenRepository
                .findByDailyRecordNotificationEnabledAndDailyNotificationTime(
                    DailyNotificationTime.NIGHT_9, 0L, PageRequest.of(0, 2),
                )
            assertThat(page1.content.map { it.userId }).containsExactly(u1.id, u2.id)
            assertThat(page1.hasNext()).isTrue()

            // 2페이지 — 마지막 커서 이후 1건, 다음 없음
            val cursor = page1.content.last().userId!!
            val page2 = userFcmTokenRepository
                .findByDailyRecordNotificationEnabledAndDailyNotificationTime(
                    DailyNotificationTime.NIGHT_9, cursor, PageRequest.of(0, 2),
                )
            assertThat(page2.content.map { it.userId }).containsExactly(u3.id)
            assertThat(page2.hasNext()).isFalse()
        }
    }
}
