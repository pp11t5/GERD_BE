package com.gerd.domain.food.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class FoodCategoryMapId(
    @Column(name = "food_id")
    val foodId: Long = 0,

    @Column(name = "food_category_id")
    val foodCategoryId: Long = 0,
) : Serializable
