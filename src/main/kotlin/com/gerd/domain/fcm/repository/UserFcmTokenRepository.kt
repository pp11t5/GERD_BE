package com.gerd.domain.fcm.repository

import com.gerd.domain.fcm.entity.UserFcmToken
import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserFcmTokenRepository : JpaRepository<UserFcmToken, Long> {

    // 발송 실패 시 만료 토큰 조회용
    fun findByToken(token: String): UserFcmToken?

    // 주간 리포트 멀티캐스트용 — 활성화된 유저의 토큰만 조회 (탈퇴 유저 명시 제외)
    @Query("""
        SELECT t.token FROM UserFcmToken t
        JOIN t.user u
        JOIN UserNotificationSetting s ON s.userId = t.userId
        WHERE s.weeklyReportEnabled = true
          AND u.deletedAt IS NULL
    """)
    fun findTokensForWeeklyReport(): List<String>

    // 매일 기록 멀티캐스트용 — 해당 시간대 활성 유저 토큰을 userId 커서로 페이징 (탈퇴 명시 제외)
    @Query("""
        SELECT t FROM UserFcmToken t
        JOIN t.user u
        JOIN UserNotificationSetting s ON s.userId = t.userId
        WHERE s.dailyNotificationTime = :time
          AND s.dailyRecordNotificationEnabled = true
          AND u.deletedAt IS NULL
          AND t.userId > :cursor
        ORDER BY t.userId ASC
    """)
    fun findByDailyRecordNotificationEnabledAndDailyNotificationTime(
        @Param("time") time: DailyNotificationTime,
        @Param("cursor") cursor: Long,
        pageable: Pageable,
    ): Slice<UserFcmToken>
}
