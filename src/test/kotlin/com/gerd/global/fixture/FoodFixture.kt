package com.gerd.global.fixture

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.FoodSearchHistory
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

object FoodFixture {

    // 검색/최근 음식 테스트가 공유하는 고정 externalId — 응답은 food.externalId와 대조하므로 값 자체는 무관
    val EXTERNAL_ID: UUID = UUID.fromString("9b1c0e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f")

    // 노출 범위 분기(source/visibility/owner)까지 표현 가능한 음식 — id는 setField로 주입
    fun food(
        id: Long = 1L,
        name: String = "된장찌개",
        source: FoodSource = FoodSource.SEED,
        visibility: FoodVisibility = FoodVisibility.PUBLIC,
        ownerUserId: Long? = null,
        externalId: UUID = EXTERNAL_ID,
    ): Food = Food(name = name, source = source, visibility = visibility, ownerUserId = ownerUserId).apply {
        ReflectionTestUtils.setField(this, "id", id)
        this.externalId = externalId // BaseEntity의 public var로 직접 할당
    }

    // 최근 본 음식 1건 — 상한/upsert/삭제 분기용
    fun history(
        id: Long,
        food: Food = food(),
        searchedAt: LocalDateTime,
        userId: Long = 1L,
    ): FoodSearchHistory = FoodSearchHistory(userId = userId, food = food, searchedAt = searchedAt).apply {
        ReflectionTestUtils.setField(this, "id", id)
    }
}
