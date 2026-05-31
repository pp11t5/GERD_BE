package com.gerd.domain.food.entity

import com.gerd.domain.food.entity.id.FoodSubstituteId
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

/**
 * 대체 음식 페어 (caution/risk 음식에 대한 대체 추천)
 *
 * - LLM 보조로 큐레이션된 정적 lookup
 */
// food_id <> substitute_food_id 제약은 DB CHECK로만 강제된다 (JPA 비이식)
@Entity
@Table(name = "food_substitutes")
class FoodSubstitute(
    @MapsId("foodId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    val food: Food,

    @MapsId("substituteFoodId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substitute_food_id", nullable = false)
    val substituteFood: Food,

    @Column(name = "reason_text")
    var reasonText: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @EmbeddedId
    val id: FoodSubstituteId = FoodSubstituteId(),
)
