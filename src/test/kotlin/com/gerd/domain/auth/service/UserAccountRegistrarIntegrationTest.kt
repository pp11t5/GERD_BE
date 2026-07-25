package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.onboarding.entity.Term
import com.gerd.domain.onboarding.repository.TermRepository
import com.gerd.domain.onboarding.repository.UserConsentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest
class UserAccountRegistrarIntegrationTest @Autowired constructor(
    private val userAccountRegistrar: UserAccountRegistrar,
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
    private val termRepository: TermRepository,
    private val userConsentRepository: UserConsentRepository,
) {

    @AfterEach
    fun tearDown() {
        userConsentRepository.deleteAll()
        authAccountRepository.deleteAll()
        userRepository.deleteAll()
        termRepository.deleteAll()
    }

    @Nested
    inner class `findOrRegister` {

        @Test
        fun `신규 가입 시 최신 약관 전체에 대한 동의 레코드가 agreed=false로 생성된다`() {
            termRepository.save(Term(code = "service", version = "1.0", title = "서비스 이용약관", content = "내용", required = true, effectiveDate = LocalDate.now()))
            termRepository.save(Term(code = "marketing", version = "1.0", title = "마케팅 수신 동의", content = "내용", required = false, effectiveDate = LocalDate.now()))

            userAccountRegistrar.findOrRegister("consent@test.com", AuthProvider.KAKAO, "kakao-consent-1") {
                User(email = "consent@test.com", nickname = "consentuser")
            }

            val user = userRepository.findByEmail("consent@test.com").get()
            val consents = userConsentRepository.findByIdUserId(user.id!!)

            assertThat(consents).hasSize(2)
            assertThat(consents).allMatch { !it.agreed }
        }

        @Test
        fun `동시에 같은 가입 요청이 오면 하나만 생성되고 나머지는 unique 충돌로 종료된다`() {
            val email = "race@test.com"
            val provider = AuthProvider.KAKAO
            val providerAccountId = "kakao-race-1"
            val ready = CountDownLatch(2)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)

            try {
                val futures = (1..2).map {
                    executor.submit(
                        Callable {
                            runCatching {
                                userAccountRegistrar.findOrRegister(email, provider, providerAccountId) {
                                    ready.countDown()
                                    start.await(5, TimeUnit.SECONDS)
                                    User(email = email, nickname = "race")
                                }
                            }
                        },
                    )
                }

                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
                start.countDown()

                val results = futures.map { it.get(5, TimeUnit.SECONDS) }
                val successes = results.mapNotNull { it.getOrNull() }
                val failures = results.mapNotNull { it.exceptionOrNull() }

                assertThat(successes).hasSize(1)
                assertThat(failures).hasSize(1)
                assertThat(failures.first()).isInstanceOf(DataIntegrityViolationException::class.java)
                assertThat(userRepository.findAll()).hasSize(1)
                assertThat(authAccountRepository.findAll()).hasSize(1)
                assertThat(authAccountRepository.findAll().first().providerAccountId).isEqualTo(providerAccountId)
            } finally {
                executor.shutdownNow()
            }
        }
    }
}
