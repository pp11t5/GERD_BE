package com.gerd.domain.onboarding.entity

import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.onboarding.entity.id.UserAllergenId
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

// 08 알레르기 칩 선택 (user ↔ allergen) — food_allergens와 대칭
@Entity
@Table(name = "user_allergens")
class UserAllergen(
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val userProfile: UserProfile,

    @MapsId("allergenId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen,

    @EmbeddedId
    val id: UserAllergenId = UserAllergenId(),
) : BaseEntity()
