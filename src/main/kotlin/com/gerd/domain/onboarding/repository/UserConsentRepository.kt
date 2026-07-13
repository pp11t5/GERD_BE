package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserConsentRepository : JpaRepository<UserConsent, UserConsentId> {
    fun findByIdUserId(userId: Long): List<UserConsent>

    // code별 최신 버전 약관에 대한 동의 단건 조회
    @Query("""
        SELECT uc FROM UserConsent uc
        JOIN uc.term t
        WHERE uc.id.userId = :userId
          AND t.code = :code
          AND t.effectiveDate = (SELECT MAX(t2.effectiveDate) FROM Term t2 WHERE t2.code = :code)
    """)
    fun findLatestByUserIdAndTermCode(@Param("userId") userId: Long, @Param("code") code: String): UserConsent?
}
