package com.gerd.domain.food.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class FoodAllergenId(
    @Column(name = "food_id")
    val foodId: Long = 0,

    @Column(name = "allergen_id")
    val allergenId: Long = 0,
) : Serializable
