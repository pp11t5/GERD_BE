package com.gerd.global.fixture

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import org.springframework.test.util.ReflectionTestUtils

object UserFixture {

    // Service 단위테스트용 — ID 있음
    fun user(): User {
        val user = User(email = "user@test.com", role = UserRole.USER)
        ReflectionTestUtils.setField(user, "id", 1L)
        return user
    }

    // 권한/소유권 분기 테스트용
    fun anotherUser(): User {
        val user = User(email = "other@test.com", role = UserRole.USER)
        ReflectionTestUtils.setField(user, "id", 2L)
        return user
    }

    // 관리자 권한 테스트용
    fun adminUser(): User {
        val user = User(email = "admin@test.com", role = UserRole.ADMIN)
        ReflectionTestUtils.setField(user, "id", 3L)
        return user
    }

    // 탈퇴 유예 상태 테스트용 — status=DELETED, deletedAt != null
    fun deletedUser(): User {
        val user = User(email = "deleted@test.com", nickname = "deleted-user", role = UserRole.USER)
        ReflectionTestUtils.setField(user, "id", 4L)
        user.withdraw()
        return user
    }
}
