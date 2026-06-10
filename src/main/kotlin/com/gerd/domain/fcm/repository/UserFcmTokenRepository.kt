package com.gerd.domain.fcm.repository

import com.gerd.domain.fcm.entity.UserFcmToken
import org.springframework.data.jpa.repository.JpaRepository

interface UserFcmTokenRepository : JpaRepository<UserFcmToken, Long> {

    // 발송 실패 시 만료 토큰 조회용
    fun findByToken(token: String): UserFcmToken?

}
