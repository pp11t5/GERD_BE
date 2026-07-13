package com.gerd.domain.onboarding.entity

import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_consents")
class UserConsent(
    @EmbeddedId
    val id: UserConsentId,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("termId")
    @JoinColumn(name = "term_id")
    val term: Term,

    @Column(nullable = false)
    var agreed: Boolean,

    @Column(name = "agreed_at", nullable = false)
    var agreedAt: LocalDateTime,
) : BaseEntity() {

    fun updateAgreement(agreed: Boolean, agreedAt: LocalDateTime) {
        this.agreed = agreed
        this.agreedAt = agreedAt
    }
}
