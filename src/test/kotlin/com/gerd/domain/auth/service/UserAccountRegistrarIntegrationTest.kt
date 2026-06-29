package com.gerd.domain.auth.service

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.repository.AuthAccountRepository
import com.gerd.domain.auth.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
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
) {

    @AfterEach
    fun tearDown() {
        authAccountRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Nested
    inner class `findOrRegister` {

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
