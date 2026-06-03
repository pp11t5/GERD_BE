package com.gerd.domain.onboarding.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class UserTriggerId(
    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(name = "trigger_label_id")
    val triggerLabelId: Long = 0,
) : Serializable
