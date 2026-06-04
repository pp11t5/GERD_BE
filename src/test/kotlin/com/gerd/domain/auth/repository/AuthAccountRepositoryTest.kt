package com.gerd.domain.auth.repository

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.global.config.QuerydslTestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(QuerydslTestConfig::class)
class AuthAccountRepositoryTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val authAccountRepository: AuthAccountRepository,
) {

    @Test
    fun `provider 와 providerAccountId 로 인증 계정을 조회한다`() {
        val user = userRepository.save(User(email = "user@test.com", role = UserRole.USER))
        val authAccount = authAccountRepository.save(
            AuthAccount(
                user = user,
                provider = AuthProvider.KAKAO,
                providerAccountId = "kakao-123",
            ),
        )

        val result = authAccountRepository.findByProviderAndProviderAccountId(
            provider = AuthProvider.KAKAO,
            providerAccountId = "kakao-123",
        )

        assertThat(result).isPresent
        assertThat(result.get().id).isEqualTo(authAccount.id)
        assertThat(result.get().user.id).isEqualTo(user.id)
    }
}
