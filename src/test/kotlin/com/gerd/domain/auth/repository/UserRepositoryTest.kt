package com.gerd.domain.auth.repository

import com.gerd.domain.auth.entity.User
import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.global.config.QuerydslTestConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class UserRepositoryTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val entityManager: EntityManager,
) {

    @Nested
    inner class `findByEmail` {

        @Test
        fun `이메일로 사용자를 조회한다`() {
            val saved = userRepository.save(User(email = "user@test.com", nickname = "user", role = UserRole.USER))

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
            userRepository.save(User(email = "user@test.com", nickname = "user", role = UserRole.USER))

            assertThat(userRepository.existsByEmail("user@test.com")).isTrue()
        }

        @Test
        fun `이메일이 없으면 false를 반환한다`() {
            assertThat(userRepository.existsByEmail("notfound@test.com")).isFalse()
        }
    }

    @Nested
    inner class `findByIdIncludingDeleted` {

        @Test
        fun `삭제된 사용자는 일반 조회에서 제외하고 우회 조회로는 찾는다`() {
            val deletedUser = userRepository.save(
                User(email = "deleted@test.com", nickname = "deleted-user", role = UserRole.USER),
            )
            deletedUser.withdraw()
            userRepository.saveAndFlush(deletedUser)
            entityManager.clear()
            val userId = deletedUser.id ?: error("saved user id must not be null")

            val normalResult = userRepository.findById(userId)
            val includingDeletedResult = userRepository.findByIdIncludingDeleted(userId)

            assertThat(normalResult).isEmpty
            assertThat(includingDeletedResult).isPresent
            assertThat(includingDeletedResult.get().status.name).isEqualTo("DELETED")
            assertThat(includingDeletedResult.get().deletedAt).isNotNull
        }
    }

    @Nested
    inner class `hardDelete` {

        @Test
        fun `삭제된 사용자를 soft delete 필터와 무관하게 물리 삭제한다`() {
            val deletedUser = userRepository.save(
                User(email = "hard-delete@test.com", nickname = "remove-me", role = UserRole.USER),
            )
            deletedUser.withdraw()
            userRepository.saveAndFlush(deletedUser)
            entityManager.clear()
            val userId = deletedUser.id ?: error("saved user id must not be null")

            userRepository.hardDelete(userId)
            entityManager.flush()
            entityManager.clear()

            val includingDeletedResult = userRepository.findByIdIncludingDeleted(userId)
            val rowCount = (entityManager.createNativeQuery("SELECT COUNT(*) FROM users WHERE user_id = ?1")
                .setParameter(1, userId)
                .singleResult as Number)
                .toLong()

            assertThat(includingDeletedResult).isEmpty
            assertThat(rowCount).isZero()
        }
    }

    @Nested
    inner class `findIdsForHardDelete` {

        // status=DELETED로 만든 뒤 deleted_at을 원하는 시각으로 고정해 저장
        private fun persistDeletedUser(email: String, deletedAt: LocalDateTime): Long {
            val user = User(email = email, nickname = "n", role = UserRole.USER)
            user.withdraw()
            ReflectionTestUtils.setField(user, "deletedAt", deletedAt)
            val id = userRepository.saveAndFlush(user).id ?: error("saved user id must not be null")
            entityManager.clear()
            return id
        }

        @Test
        fun `유예기간이 지난 DELETED 유저만 조회한다`() {
            val threshold = LocalDateTime.now().minusDays(14)
            val expired = persistDeletedUser("expired@test.com", LocalDateTime.now().minusDays(15))
            persistDeletedUser("recent@test.com", LocalDateTime.now().minusDays(13)) // 유예 이내 → 제외
            userRepository.saveAndFlush( // ACTIVE → 제외
                User(email = "active@test.com", nickname = "active", role = UserRole.USER),
            )
            entityManager.clear()

            val result = userRepository.findIdsForHardDelete(threshold, 0L, 100)

            assertThat(result).containsExactly(expired)
        }

        @Test
        fun `lastId 커서 이후의 유저만 조회한다`() {
            val threshold = LocalDateTime.now().minusDays(14)
            val first = persistDeletedUser("first@test.com", LocalDateTime.now().minusDays(20))
            val second = persistDeletedUser("second@test.com", LocalDateTime.now().minusDays(20))

            val result = userRepository.findIdsForHardDelete(threshold, first, 100)

            assertThat(result).containsExactly(second)
        }

        @Test
        fun `limit 개수만큼 user_id 오름차순으로 조회한다`() {
            val threshold = LocalDateTime.now().minusDays(14)
            val id1 = persistDeletedUser("u1@test.com", LocalDateTime.now().minusDays(20))
            val id2 = persistDeletedUser("u2@test.com", LocalDateTime.now().minusDays(20))
            persistDeletedUser("u3@test.com", LocalDateTime.now().minusDays(20))

            val result = userRepository.findIdsForHardDelete(threshold, 0L, 2)

            assertThat(result).containsExactly(id1, id2)
        }
    }
}
