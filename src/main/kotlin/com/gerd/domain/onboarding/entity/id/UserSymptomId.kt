package com.gerd.domain.onboarding.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class UserSymptomId(
    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(name = "symptom_code")
    val symptomCode: String = "",
) : Serializable
