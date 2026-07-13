package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.Term
import com.gerd.global.config.QuerydslTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class TermRepositoryTest @Autowired constructor(
    private val termRepository: TermRepository,
) {

    private fun term(code: String, version: String, effectiveDate: LocalDate, required: Boolean = true) = Term(
        code = code,
        version = version,
        title = "${code} 약관",
        content = "${code} 내용",
        required = required,
        effectiveDate = effectiveDate,
    )

    @Nested
    inner class `findLatestAll` {

        @Test
        fun `code별 effectiveDate가 가장 최신인 약관만 반환한다`() {
            val base = LocalDate.of(2026, 1, 1)
            termRepository.save(term("tos", "1.0", base))
            termRepository.save(term("tos", "2.0", base.plusMonths(6)))
            termRepository.save(term("privacy", "1.0", base))

            val result = termRepository.findLatestAll()

            assertThat(result).hasSize(2)
            assertThat(result.first { it.code == "tos" }.version).isEqualTo("2.0")
            assertThat(result.first { it.code == "privacy" }.version).isEqualTo("1.0")
        }

        @Test
        fun `약관이 없으면 빈 리스트를 반환한다`() {
            assertThat(termRepository.findLatestAll()).isEmpty()
        }
    }
}
