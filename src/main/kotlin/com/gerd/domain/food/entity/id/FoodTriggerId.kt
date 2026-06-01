package com.gerd.domain.food.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

// 복합키는 equals/hashCode가 필요해 data class로 둔다 (data class 금지 규칙은 @Entity 본체에만 적용)
@Embeddable
data class FoodTriggerId(
    @Column(name = "food_id")
    val foodId: Long = 0,

    @Column(name = "trigger_label_id")
    val triggerLabelId: Long = 0,
) : Serializable
