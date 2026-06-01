package com.gerd.domain.food.entity

import com.gerd.domain.food.entity.id.FoodTriggerId
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

// food ↔ trigger_label M:N 조인 (한 음식이 가진 역류 유발 요인)
@Entity
@Table(name = "food_triggers")
class FoodTrigger(
    @MapsId("foodId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    val food: Food,

    @MapsId("triggerLabelId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trigger_label_id", nullable = false)
    val triggerLabel: TriggerLabel,

    @EmbeddedId
    val id: FoodTriggerId = FoodTriggerId(),
) : BaseEntity()
