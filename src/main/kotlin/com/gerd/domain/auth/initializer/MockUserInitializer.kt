package com.gerd.domain.auth.initializer

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 *  개발 및 테스트 환경에서 사용할 Mock 사용자 초기화 클래스
 *
 *  애플리케이션이 시작될 때 자동으로 실행되어 지정된 이메일과 역할을 가진 Mock 사용자를 데이터베이스에 저장합니다.
 *  프로덕션 환경에서는 이 클래스가 활성화되지 않습니다.
 */
@Component
@Profile("local", "test")
class MockUserInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    // 개발용 고정 비밀번호 — 로컬/테스트 전용
    private val devPassword = "dev1234!"

    override fun run(vararg args: String) {
        seedUser("dev-user@gerd.local", UserRole.USER)
        seedUser("dev-admin@gerd.local", UserRole.ADMIN)
    }

    private fun seedUser(email: String, role: UserRole) {
        if (userRepository.existsByEmail(email)) {
            return
        }

        val user = User(email = email, role = role)
        user.encodePassword(passwordEncoder.encode(devPassword))
        userRepository.save(user)
        log.info("Mock user initialized: email={}, role={}, password={}", email, role, devPassword)
    }
}
