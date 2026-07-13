package com.gerd.domain.onboarding.entity.id

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class UserConsentId(
    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(name = "term_id")
    val termId: Long = 0,
) : Serializable
