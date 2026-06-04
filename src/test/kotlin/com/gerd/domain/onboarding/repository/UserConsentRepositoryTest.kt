package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.LocalDateTime

@DataJpaTest
class UserConsentRepositoryTest @Autowired constructor(
    private val userConsentRepository: UserConsentRepository,
) {

    @Nested
    inner class `findByIdUserId` {

        @Test
        fun `사용자의 동의 행만 모두 조회한다`() {
            val now = LocalDateTime.now()
            userConsentRepository.save(UserConsent(UserConsentId(1L, "tos"), agreed = true, agreedAt = now))
            userConsentRepository.save(UserConsent(UserConsentId(1L, "privacy"), agreed = true, agreedAt = now))
            userConsentRepository.save(UserConsent(UserConsentId(2L, "tos"), agreed = true, agreedAt = now))

            val result = userConsentRepository.findByIdUserId(1L)

            assertThat(result).hasSize(2)
            assertThat(result.map { it.id.consentType }).containsExactlyInAnyOrder("tos", "privacy")
        }

        @Test
        fun `동의 행이 없으면 빈 리스트를 반환한다`() {
            val result = userConsentRepository.findByIdUserId(99L)

            assertThat(result).isEmpty()
        }
    }
}
