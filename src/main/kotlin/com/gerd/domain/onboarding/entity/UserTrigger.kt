package com.gerd.domain.onboarding.entity

import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.domain.onboarding.entity.id.UserTriggerId
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

// 07 트리거 칩 선택 (user ↔ trigger_label) — food_triggers와 대칭이라 F2 매칭이 집합 교집합으로 깔끔
@Entity
@Table(name = "user_triggers")
class UserTrigger(
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val userProfile: UserProfile,

    @MapsId("triggerLabelId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trigger_label_id", nullable = false)
    val triggerLabel: TriggerLabel,

    @EmbeddedId
    val id: UserTriggerId = UserTriggerId(),
) : BaseEntity()
