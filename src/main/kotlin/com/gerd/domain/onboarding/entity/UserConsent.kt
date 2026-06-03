package com.gerd.domain.onboarding.entity

import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 약관동의 03 (users 직접 참조, type별 현재 상태 1행)
 *
 * - 동의는 온보딩 제출 이전(로그인 직후) 발생이라 user_profiles에 의존하지 않음
 * - version·이력 없음 — 마케팅 철회는 agreed=false update
 */
@Entity
@Table(name = "user_consents")
class UserConsent(
    @EmbeddedId
    val id: UserConsentId,

    @Column(nullable = false)
    var agreed: Boolean,

    @Column(name = "agreed_at", nullable = false)
    var agreedAt: LocalDateTime,
) : BaseEntity() {

    // 재동의·철회 시 현재 상태와 시점을 함께 갱신
    fun updateAgreement(agreed: Boolean, agreedAt: LocalDateTime) {
        this.agreed = agreed
        this.agreedAt = agreedAt
    }
}
