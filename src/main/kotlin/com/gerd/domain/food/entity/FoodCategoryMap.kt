package com.gerd.domain.food.entity

import com.gerd.domain.food.entity.id.FoodCategoryMapId
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

// food ↔ food_category M:N 조인 (한 음식이 복합 메뉴일 때 여러 분류를 가짐)
@Entity
@Table(name = "food_category_maps")
class FoodCategoryMap(
    @MapsId("foodId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    val food: Food,

    @MapsId("foodCategoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_category_id", nullable = false)
    val foodCategory: FoodCategory,

    @EmbeddedId
    val id: FoodCategoryMapId = FoodCategoryMapId(),
)
