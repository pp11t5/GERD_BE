package com.gerd.domain.auth.service

import com.gerd.domain.auth.repository.RefreshTokenRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class RefreshTokenRevoker(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    // REQUIRES_NEW — 호출 트랜잭션이 롤백돼도 삭제는 독립 커밋으로 보장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun revokeAllSessions(userId: Long) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }
}
