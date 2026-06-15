package com.gerd.domain.notification.entity

import com.gerd.domain.auth.entity.User
import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import com.gerd.domain.notification.entity.enums.NotificationSettingType
import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "user_notification_settings")
class UserNotificationSetting(

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Id
    @Column(name = "user_id")
    val userId: Long? = null,

    postMealNotificationEnabled: Boolean = true,
    dailyRecordNotificationEnabled: Boolean = true,
    dailyNotificationTime: DailyNotificationTime = DailyNotificationTime.NIGHT_9,
    weeklyReportEnabled: Boolean = true,

) : BaseTimeEntity() {

    @Column(name = "post_meal_notification_enabled", nullable = false)
    var postMealNotificationEnabled: Boolean = postMealNotificationEnabled
        protected set

    @Column(name = "daily_record_notification_enabled", nullable = false)
    var dailyRecordNotificationEnabled: Boolean = dailyRecordNotificationEnabled
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "daily_notification_time", nullable = false, length = 20)
    var dailyNotificationTime: DailyNotificationTime = dailyNotificationTime
        protected set

    @Column(name = "weekly_report_enabled", nullable = false)
    var weeklyReportEnabled: Boolean = weeklyReportEnabled
        protected set

    fun update(
        postMealNotificationEnabled: Boolean,
        dailyRecordNotificationEnabled: Boolean,
        dailyNotificationTime: DailyNotificationTime,
        weeklyReportEnabled: Boolean,
    ) {
        this.postMealNotificationEnabled = postMealNotificationEnabled
        this.dailyRecordNotificationEnabled = dailyRecordNotificationEnabled
        this.dailyNotificationTime = dailyNotificationTime
        this.weeklyReportEnabled = weeklyReportEnabled
    }

    fun updateDailyNotificationTime(dailyNotificationTime: DailyNotificationTime) {
        this.dailyNotificationTime = dailyNotificationTime
    }
    fun updatePostMealNotificationEnabled(postMealNotificationEnabled: Boolean) {
        this.postMealNotificationEnabled = postMealNotificationEnabled
    }
    fun updateDailyRecordNotificationEnabled(dailyRecordNotificationEnabled: Boolean) {
        this.dailyRecordNotificationEnabled = dailyRecordNotificationEnabled
    }
    fun updateWeeklyReportEnabled(weeklyReportEnabled: Boolean) {
        this.weeklyReportEnabled = weeklyReportEnabled
    }

    // 토글 종류별 활성 여부 조회
    fun isEnabled(type: NotificationSettingType): Boolean = when (type) {
        NotificationSettingType.POST_MEAL -> postMealNotificationEnabled
        NotificationSettingType.DAILY_RECORD -> dailyRecordNotificationEnabled
        NotificationSettingType.WEEKLY_REPORT -> weeklyReportEnabled
    }
}
