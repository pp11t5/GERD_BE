package com.gerd.domain.dictionary.repository

import com.gerd.domain.auth.entity.User
import com.gerd.domain.dictionary.entity.UserFoodDictionary
import com.gerd.domain.dictionary.entity.enums.DictionaryType
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
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslTestConfig::class)
class UserFoodDictionaryRepositoryTest @Autowired constructor(
    private val dictionaryRepository: UserFoodDictionaryRepository,
    private val em: EntityManager,
) {

    @Nested
    inner class `커서 페이징 - SAFE 단일 타입` {

        @Test
        fun `cursor가 없으면 최신순으로 첫 페이지를 반환한다`() {
            val user = saveUser()
            val food1 = saveFood("음식A")
            val food2 = saveFood("음식B")
            val entry1 = dictionaryRepository.save(entry(user, food1, DictionaryType.SAFE))
            val entry2 = dictionaryRepository.save(entry(user, food2, DictionaryType.SAFE))
            em.flush(); em.clear()

            val result = dictionaryRepository.findWithFoodByCursorAndType(
                user.id!!, DictionaryType.SAFE, cursor = null, PageRequest.of(0, 10),
            )

            assertThat(result).hasSize(2)
            assertThat(result.map { it.id }).containsExactly(entry2.id, entry1.id)
        }

        @Test
        fun `cursor보다 id가 작은 항목만 반환한다`() {
            val user = saveUser("cursor@test.com")
            val food1 = saveFood("음식C")
            val food2 = saveFood("음식D")
            val food3 = saveFood("음식E")
            val entry1 = dictionaryRepository.save(entry(user, food1, DictionaryType.SAFE))
            val entry2 = dictionaryRepository.save(entry(user, food2, DictionaryType.SAFE))
            val entry3 = dictionaryRepository.save(entry(user, food3, DictionaryType.SAFE))
            em.flush(); em.clear()

            val result = dictionaryRepository.findWithFoodByCursorAndType(
                user.id!!, DictionaryType.SAFE, cursor = entry3.id, PageRequest.of(0, 10),
            )

            // cursor(entry3.id)보다 id가 작은 항목 전부를 최신순으로 반환한다(entry3 제외)
            assertThat(result.map { it.id }).containsExactly(entry2.id, entry1.id)
        }

        @Test
        fun `JOIN FETCH로 Food가 함께 로딩된다`() {
            val user = saveUser("fetch@test.com")
            val food = saveFood("현미밥")
            dictionaryRepository.save(entry(user, food, DictionaryType.SAFE))
            em.flush(); em.clear()

            val result = dictionaryRepository.findWithFoodByCursorAndType(
                user.id!!, DictionaryType.SAFE, cursor = null, PageRequest.of(0, 10),
            )

            assertThat(result).hasSize(1)
            assertThat(result.first().food.name).isEqualTo("현미밥")
        }

        @Test
        fun `다른 타입은 반환하지 않는다`() {
            val user = saveUser("type@test.com")
            val food = saveFood("김치")
            dictionaryRepository.save(entry(user, food, DictionaryType.CAUTION))
            em.flush(); em.clear()

            val result = dictionaryRepository.findWithFoodByCursorAndType(
                user.id!!, DictionaryType.SAFE, cursor = null, PageRequest.of(0, 10),
            )

            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class `커서 페이징 - CAUTION·RISK 복합 타입` {

        @Test
        fun `CAUTION과 RISK를 함께 최신순으로 반환한다`() {
            val user = saveUser("multi@test.com")
            val food1 = saveFood("된장찌개")
            val food2 = saveFood("고추장")
            val caution = dictionaryRepository.save(entry(user, food1, DictionaryType.CAUTION))
            val risk = dictionaryRepository.save(entry(user, food2, DictionaryType.RISK))
            em.flush(); em.clear()

            val result = dictionaryRepository.findWithFoodByCursorAndTypeIn(
                user.id!!, listOf(DictionaryType.CAUTION, DictionaryType.RISK),
                cursor = null, PageRequest.of(0, 10),
            )

            assertThat(result).hasSize(2)
            assertThat(result.map { it.id }).containsExactly(risk.id, caution.id)
        }

        @Test
        fun `SAFE 타입은 제외된다`() {
            val user = saveUser("safe-ex@test.com")
            val food = saveFood("두부")
            dictionaryRepository.save(entry(user, food, DictionaryType.SAFE))
            em.flush(); em.clear()

            val result = dictionaryRepository.findWithFoodByCursorAndTypeIn(
                user.id!!, listOf(DictionaryType.CAUTION, DictionaryType.RISK),
                cursor = null, PageRequest.of(0, 10),
            )

            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class `카운트` {

        @Test
        fun `타입별 개수를 정확히 센다`() {
            val user = saveUser("count@test.com")
            val food1 = saveFood("파스타")
            val food2 = saveFood("라면")
            val food3 = saveFood("삼겹살")
            dictionaryRepository.save(entry(user, food1, DictionaryType.SAFE))
            dictionaryRepository.save(entry(user, food2, DictionaryType.CAUTION))
            dictionaryRepository.save(entry(user, food3, DictionaryType.RISK))
            em.flush(); em.clear()

            val safeCount = dictionaryRepository.countByUser_IdAndDictionaryType(user.id!!, DictionaryType.SAFE)
            val cautionRiskCount = dictionaryRepository.countByUser_IdAndDictionaryTypeIn(
                user.id!!, listOf(DictionaryType.CAUTION, DictionaryType.RISK),
            )

            assertThat(safeCount).isEqualTo(1L)
            assertThat(cautionRiskCount).isEqualTo(2L)
        }
    }

    @Nested
    inner class `타입별 일괄 삭제` {

        @Test
        fun `지정 foodId와 타입에 해당하는 항목만 삭제한다`() {
            val user = saveUser("del@test.com")
            val food1 = saveFood("삭제대상")
            val food2 = saveFood("유지대상")
            dictionaryRepository.save(entry(user, food1, DictionaryType.SAFE))
            val kept = dictionaryRepository.save(entry(user, food2, DictionaryType.SAFE))
            em.flush(); em.clear()

            dictionaryRepository.deleteByUserIdAndFoodIdsAndType(user.id!!, listOf(food1.id!!), DictionaryType.SAFE)
            em.flush(); em.clear()

            val remaining = dictionaryRepository.findAll()
            assertThat(remaining).hasSize(1)
            assertThat(remaining.first().id).isEqualTo(kept.id)
        }

        @Test
        fun `다른 타입의 항목은 삭제하지 않는다`() {
            val user = saveUser("del-type@test.com")
            val food = saveFood("공통음식")
            dictionaryRepository.save(entry(user, food, DictionaryType.SAFE))
            val caution = dictionaryRepository.save(entry(user, food, DictionaryType.CAUTION))
            em.flush(); em.clear()

            dictionaryRepository.deleteByUserIdAndFoodIdsAndType(user.id!!, listOf(food.id!!), DictionaryType.SAFE)
            em.flush(); em.clear()

            val remaining = dictionaryRepository.findAll()
            assertThat(remaining).hasSize(1)
            assertThat(remaining.first().id).isEqualTo(caution.id)
        }
    }

    private fun saveUser(email: String = "user@test.com"): User =
        User(email = email).also {
            em.persist(it)
            em.flush()
        }

    private fun saveFood(name: String): Food =
        Food(name = name, source = FoodSource.SEED, visibility = FoodVisibility.PUBLIC).also {
            em.persist(it)
            em.flush()
        }

    private fun entry(user: User, food: Food, type: DictionaryType) =
        UserFoodDictionary(user = user, food = food, dictionaryType = type)
}
