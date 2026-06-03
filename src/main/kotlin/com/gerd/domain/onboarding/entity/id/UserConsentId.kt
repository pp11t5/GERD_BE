package com.gerd.domain.onboarding.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class UserConsentId(
    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(name = "consent_type")
    val consentType: String = "",
) : Serializable
