package com.gerd.domain.auth.initializer

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 *  개발 및 테스트 환경에서 사용할 Mock 사용자 초기화 클래스
 *
 *  애플리케이션이 시작될 때 자동으로 실행되어 닉네임과 역할을 가진 Mock 사용자를 데이터베이스에 저장합니다.
 *  프로덕션 환경에서는 이 클래스가 활성화되지 않습니다.
 */
@Component
@Profile("local", "test")
class MockUserInitializer(
    private val userRepository: UserRepository,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        seedUser(email = "dev-user@gerd.local", nickname = "dev-user", role = UserRole.USER)
        seedUser(email = "dev-admin@gerd.local", nickname = "dev-admin", role = UserRole.ADMIN)
    }

    private fun seedUser(email: String, nickname: String, role: UserRole) {
        if (userRepository.existsByEmail(email)) {
            return
        }

        val user = User(email = email, nickname = nickname, role = role)
        userRepository.save(user)
        log.info("Mock user initialized: nickname={}, role={}", nickname, role)
    }
}
