package com.gerd.domain.auth.repository

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

@DataJpaTest
class UserRepositoryTest @Autowired constructor(
    private val userRepository: UserRepository,
) {

    @Nested
    inner class `findByEmail` {

        @Test
        fun `이메일로 사용자를 조회한다`() {
            val saved = userRepository.save(User(email = "user@test.com", role = UserRole.USER))

            val result = userRepository.findByEmail("user@test.com")

            assertThat(result).isPresent
            assertThat(result.get().id).isEqualTo(saved.id)
            assertThat(result.get().email).isEqualTo("user@test.com")
        }

        @Test
        fun `존재하지 않는 이메일이면 empty를 반환한다`() {
            val result = userRepository.findByEmail("notfound@test.com")

            assertThat(result).isEmpty
        }
    }

    @Nested
    inner class `existsByEmail` {

        @Test
        fun `이메일이 존재하면 true를 반환한다`() {
            userRepository.save(User(email = "user@test.com", role = UserRole.USER))

            assertThat(userRepository.existsByEmail("user@test.com")).isTrue()
        }

        @Test
        fun `이메일이 없으면 false를 반환한다`() {
            assertThat(userRepository.existsByEmail("notfound@test.com")).isFalse()
        }
    }
}
