package com.gerd.domain.notification.repository

import com.gerd.domain.notification.entity.UserNotificationSetting
import org.springframework.data.jpa.repository.JpaRepository

interface UserNotificationSettingRepository : JpaRepository<UserNotificationSetting, Long>
