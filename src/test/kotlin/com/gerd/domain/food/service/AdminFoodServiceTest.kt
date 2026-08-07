package com.gerd.domain.food.service

import com.gerd.domain.dictionary.repository.UserFoodDictionaryRepository
import com.gerd.domain.food.entity.UserFood
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.domain.food.exception.FoodErrorCode
import com.gerd.domain.food.repository.FoodRepository
import com.gerd.domain.food.repository.UserFoodRepository
import com.gerd.global.apiPayload.GeneralException
import com.gerd.global.fixture.FoodFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminFoodServiceTest {

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var userFoodRepository: UserFoodRepository

    @Mock
    private lateinit var dictionaryRepository: UserFoodDictionaryRepository

    @InjectMocks
    private lateinit var service: AdminFoodService

    @Nested
    inner class getAllUserFoods {

        @Test
        fun `isUnknown 없으면 전체 UserFood를 조회한다`() {
            val food = FoodFixture.food(id = 1, name = "된장찌개")
            val userFood = UserFood(id = 10L, userId = 1L, food = food, isUnknown = true)
            val page = PageImpl(listOf(userFood), PageRequest.of(0, 100), 1)
            whenever(userFoodRepository.findAllWithFood(any())).thenReturn(page)

            val result = service.getAllUserFoods(0, null)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("된장찌개")
            assertThat(result.content[0].externalId).isEqualTo(food.externalId.toString())
            verify(userFoodRepository).findAllWithFood(any())
            verify(userFoodRepository, never()).findAllWithFoodByIsUnknown(any(), any())
        }

        @Test
        fun `isUnknown=true면 unknown 음식만 조회한다`() {
            val food = FoodFixture.food(id = 2, name = "집밥")
            val userFood = UserFood(id = 20L, userId = 1L, food = food, isUnknown = true)
            val page = PageImpl(listOf(userFood), PageRequest.of(0, 100), 1)
            whenever(userFoodRepository.findAllWithFoodByIsUnknown(eq(true), any())).thenReturn(page)

            val result = service.getAllUserFoods(0, true)

            assertThat(result.content).hasSize(1)
            verify(userFoodRepository).findAllWithFoodByIsUnknown(eq(true), any())
            verify(userFoodRepository, never()).findAllWithFood(any())
        }

        @Test
        fun `isUnknown=false면 known 음식만 조회한다`() {
            val food = FoodFixture.food(id = 3, name = "김치찌개")
            val userFood = UserFood(id = 30L, userId = 2L, food = food, isUnknown = false)
            val page = PageImpl(listOf(userFood), PageRequest.of(0, 100), 1)
            whenever(userFoodRepository.findAllWithFoodByIsUnknown(eq(false), any())).thenReturn(page)

            val result = service.getAllUserFoods(0, false)

            assertThat(result.content).hasSize(1)
            verify(userFoodRepository).findAllWithFoodByIsUnknown(eq(false), any())
        }

        @Test
        fun `결과가 없으면 빈 목록을 반환한다`() {
            val page = PageImpl<UserFood>(emptyList(), PageRequest.of(0, 100), 0)
            whenever(userFoodRepository.findAllWithFood(any())).thenReturn(page)

            val result = service.getAllUserFoods(0, null)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        fun `페이지 메타 정보가 올바르게 매핑된다`() {
            val food = FoodFixture.food(id = 1, name = "된장찌개")
            val userFoods = (1..3).map { UserFood(id = it.toLong(), userId = 1L, food = food, isUnknown = true) }
            val page = PageImpl(userFoods, PageRequest.of(1, 100), 203)
            whenever(userFoodRepository.findAllWithFood(any())).thenReturn(page)

            val result = service.getAllUserFoods(1, null)

            assertThat(result.page).isEqualTo(1)
            assertThat(result.size).isEqualTo(100)
            assertThat(result.totalElements).isEqualTo(203)
            assertThat(result.totalPages).isEqualTo(3)
        }
    }

    @Nested
    inner class promote {

        @Test
        fun `형식이 잘못된 UUID면 FOOD_NOT_FOUND`() {
            assertThatThrownBy { service.promote("not-a-uuid") }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
            verify(foodRepository, never()).findByExternalId(any())
        }

        @Test
        fun `존재하지 않는 음식이면 FOOD_NOT_FOUND`() {
            val uuid = UUID.randomUUID()
            whenever(foodRepository.findByExternalId(uuid)).thenReturn(null)

            assertThatThrownBy { service.promote(uuid.toString()) }
                .isInstanceOf(GeneralException::class.java)
                .extracting("errorCode").isEqualTo(FoodErrorCode.FOOD_NOT_FOUND)
        }

        @Test
        fun `승격 시 연결된 UserFood를 모두 삭제하고 음식을 CURATED-PUBLIC으로 변경한다`() {
            val uuid = FoodFixture.EXTERNAL_ID
            val food = FoodFixture.food(id = 7, source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, externalId = uuid)
            whenever(foodRepository.findByExternalId(uuid)).thenReturn(food)

            service.promote(uuid.toString())

            verify(userFoodRepository).deleteAllByFoodId(7L)
            assertThat(food.source).isEqualTo(FoodSource.CURATED)
            assertThat(food.visibility).isEqualTo(FoodVisibility.PUBLIC)
        }

        @Test
        fun `동일 이름의 다른 유저 USER 음식이 있으면 도감을 이전하고 소프트 삭제한다`() {
            val uuid = FoodFixture.EXTERNAL_ID
            val food = FoodFixture.food(id = 7, name = "된장찌개", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, externalId = uuid)
            val duplicate = FoodFixture.food(id = 9, name = "된장찌개", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE)
            whenever(foodRepository.findByExternalId(uuid)).thenReturn(food)
            whenever(foodRepository.findByNameAndSourceAndIdNot("된장찌개", FoodSource.USER, 7L))
                .thenReturn(listOf(duplicate))

            service.promote(uuid.toString())

            verify(dictionaryRepository).migrateFoodId(9L, 7L)
            verify(dictionaryRepository).deleteAllByFoodId(9L)
            verify(userFoodRepository).deleteAllByFoodId(9L)
            verify(foodRepository).delete(duplicate)
            verify(userFoodRepository).deleteAllByFoodId(7L)
        }

        @Test
        fun `동일 이름의 중복이 없으면 병합 없이 승격만 한다`() {
            val uuid = FoodFixture.EXTERNAL_ID
            val food = FoodFixture.food(id = 7, name = "된장찌개", source = FoodSource.USER, visibility = FoodVisibility.PRIVATE, externalId = uuid)
            whenever(foodRepository.findByExternalId(uuid)).thenReturn(food)
            whenever(foodRepository.findByNameAndSourceAndIdNot("된장찌개", FoodSource.USER, 7L))
                .thenReturn(emptyList())

            service.promote(uuid.toString())

            verify(foodRepository, never()).delete(any())
            verify(dictionaryRepository, never()).migrateFoodId(any(), any())
        }

        @Test
        fun `이미 CURATED인 음식도 promote를 호출할 수 있다`() {
            val uuid = FoodFixture.EXTERNAL_ID
            val food = FoodFixture.food(id = 8, source = FoodSource.CURATED, visibility = FoodVisibility.PUBLIC, externalId = uuid)
            whenever(foodRepository.findByExternalId(uuid)).thenReturn(food)

            service.promote(uuid.toString())

            verify(userFoodRepository).deleteAllByFoodId(8L)

            assertThat(food.source).isEqualTo(FoodSource.CURATED)
            assertThat(food.visibility).isEqualTo(FoodVisibility.PUBLIC)
        }
    }
}
