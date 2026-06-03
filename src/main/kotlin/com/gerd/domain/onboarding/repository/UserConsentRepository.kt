package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import org.springframework.data.jpa.repository.JpaRepository

interface UserConsentRepository : JpaRepository<UserConsent, UserConsentId> {
    // type별 upsert를 위해 사용자의 현재 동의 행을 한 번에 조회
    fun findByIdUserId(userId: Long): List<UserConsent>
}
