package com.gerd.domain.food.entity

import com.gerd.domain.food.entity.id.FoodAllergenId
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

// food ↔ allergen M:N 조인 (한 음식이 포함한 알레르기 유발 식품)
@Entity
@Table(name = "food_allergens")
class FoodAllergen(
    @MapsId("foodId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    val food: Food,

    @MapsId("allergenId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen,

    @EmbeddedId
    val id: FoodAllergenId = FoodAllergenId(),
) : BaseEntity()
