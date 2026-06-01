package com.gerd.domain.food.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class FoodSubstituteId(
    @Column(name = "food_id")
    val foodId: Long = 0,

    @Column(name = "substitute_food_id")
    val substituteFoodId: Long = 0,
) : Serializable
