package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.Term
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.global.config.QuerydslTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class UserConsentRepositoryTest @Autowired constructor(
    private val userConsentRepository: UserConsentRepository,
    private val termRepository: TermRepository,
) {

    private fun savedTerm(code: String) = termRepository.save(
        Term(code = code, version = "1.0", title = "${code} 약관", content = "내용", required = true, effectiveDate = LocalDate.now()),
    )

    @Nested
    inner class `findLatestByUserIdAndTermCode` {

        @Test
        fun `최신 버전 약관의 동의 내역을 반환한다`() {
            val effectiveDate = LocalDate.now()
            val oldTos = termRepository.save(
                Term(code = "tos", version = "1.0", title = "약관", content = "내용", required = true, effectiveDate = effectiveDate.minusMonths(6)),
            )
            val newTos = termRepository.save(
                Term(code = "tos", version = "2.0", title = "약관", content = "내용", required = true, effectiveDate = effectiveDate),
            )
            val now = LocalDateTime.now()
            userConsentRepository.save(UserConsent(UserConsentId(1L, oldTos.id), oldTos, agreed = true, agreedAt = now))
            userConsentRepository.save(UserConsent(UserConsentId(1L, newTos.id), newTos, agreed = false, agreedAt = now))

            val result = userConsentRepository.findLatestByUserIdAndTermCode(1L, "tos")

            assertThat(result).isNotNull
            assertThat(result!!.id.termId).isEqualTo(newTos.id)
            assertThat(result.agreed).isFalse()
        }

        @Test
        fun `해당 code의 동의 내역이 없으면 null을 반환한다`() {
            val result = userConsentRepository.findLatestByUserIdAndTermCode(1L, "marketing")

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class `findByIdUserId` {

        @Test
        fun `사용자의 동의 행만 모두 조회한다`() {
            val tos = savedTerm("tos")
            val privacy = savedTerm("privacy")
            val now = LocalDateTime.now()
            userConsentRepository.save(UserConsent(UserConsentId(1L, tos.id), tos, agreed = true, agreedAt = now))
            userConsentRepository.save(UserConsent(UserConsentId(1L, privacy.id), privacy, agreed = true, agreedAt = now))
            userConsentRepository.save(UserConsent(UserConsentId(2L, tos.id), tos, agreed = true, agreedAt = now))

            val result = userConsentRepository.findByIdUserId(1L)

            assertThat(result).hasSize(2)
            assertThat(result.map { it.id.termId }).containsExactlyInAnyOrder(tos.id, privacy.id)
        }

        @Test
        fun `동의 행이 없으면 빈 리스트를 반환한다`() {
            assertThat(userConsentRepository.findByIdUserId(99L)).isEmpty()
        }
    }
}
