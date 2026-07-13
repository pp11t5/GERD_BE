package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.Term
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TermRepository : JpaRepository<Term, Long> {

    // code별 effectiveDate가 가장 최근인 버전만 조회
    @Query("""
        SELECT t FROM Term t
        WHERE t.effectiveDate = (
            SELECT MAX(t2.effectiveDate) FROM Term t2 WHERE t2.code = t.code AND t2.effectiveDate <= CURRENT_DATE
        )
        ORDER BY t.code
    """)
    fun findLatestAll(): List<Term>
}
