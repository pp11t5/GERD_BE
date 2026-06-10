package com.gerd.domain.food.service

import com.gerd.domain.food.entity.Food
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility

// 음식 노출 범위 정책: 공개 카탈로그 ∪ 본인 비공개 음식 (최근 본 음식·신호등 판정 공용)
object FoodAccessPolicy {

    private val PUBLIC_SOURCES = setOf(FoodSource.SEED, FoodSource.CURATED)

    fun isVisibleTo(food: Food, userId: Long): Boolean =
        (food.source in PUBLIC_SOURCES && food.visibility == FoodVisibility.PUBLIC) ||
            (food.visibility == FoodVisibility.PRIVATE && food.ownerUserId == userId)
}
