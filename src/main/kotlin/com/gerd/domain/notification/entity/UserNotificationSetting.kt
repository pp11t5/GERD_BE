package com.gerd.domain.notification.entity

import com.gerd.domain.auth.entity.User
import com.gerd.domain.notification.entity.enums.DailyNotificationTime
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

    @Column(name = "post_meal_notification_enabled", nullable = false)
    var postMealNotificationEnabled: Boolean = true,

    @Column(name = "daily_record_notification_enabled", nullable = false)
    var dailyRecordNotificationEnabled: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "daily_notification_time", nullable = false, length = 20)
    var dailyNotificationTime: DailyNotificationTime = DailyNotificationTime.NIGHT_9,

    @Column(name = "weekly_report_enabled", nullable = false)
    var weeklyReportEnabled: Boolean = true,

) : BaseTimeEntity() {

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
}
