package com.gerd.domain.food.repository

import com.gerd.domain.auth.entity.User
import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.global.config.QuerydslTestConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class UserFoodRepositoryTest @Autowired constructor(
    private val userFoodRepository: UserFoodRepository,
    private val em: EntityManager,
) {

    @Nested
    inner class `유저-음식 매핑 등록` {

        @Test
        fun `없으면 새로 등록한다`() {
            val user = saveUser()
            val food = saveUserFood(user, "새우깡")

            userFoodRepository.insertIfAbsent(user.id!!, food.id!!, isUnknown = true)
            em.flush(); em.clear()

            val entries = userFoodRepository.findAll()
            assertThat(entries).hasSize(1)
            assertThat(entries.single().isUnknown).isTrue()
        }

        @Test
        fun `이미 있으면 중복 없이 무시한다`() {
            val user = saveUser()
            val food = saveUserFood(user, "감자칩")
            userFoodRepository.insertIfAbsent(user.id!!, food.id!!, isUnknown = true)
            em.flush(); em.clear()

            userFoodRepository.insertIfAbsent(user.id!!, food.id!!, isUnknown = true)
            em.flush(); em.clear()

            assertThat(userFoodRepository.findAll()).hasSize(1)
        }
    }

    private fun saveUser(email: String = "user@test.com"): User =
        User(email = email, nickname = email.substringBefore("@")).also {
            em.persist(it)
            em.flush()
        }

    private fun saveUserFood(owner: User, name: String): Food =
        Food(name = name, source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, ownerUserId = owner.id).also {
            em.persist(it)
            em.flush()
        }
}
