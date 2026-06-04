package com.gerd.domain.onboarding.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class UserAllergenId(
    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(name = "allergen_id")
    val allergenId: Long = 0,
) : Serializable
