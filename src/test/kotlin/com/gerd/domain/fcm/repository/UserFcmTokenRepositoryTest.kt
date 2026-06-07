package com.gerd.domain.fcm.repository

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.global.config.QuerydslTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(QuerydslTestConfig::class)
class UserFcmTokenRepositoryTest @Autowired constructor(
    private val userFcmTokenRepository: UserFcmTokenRepository,
    private val userRepository: UserRepository,
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
}
