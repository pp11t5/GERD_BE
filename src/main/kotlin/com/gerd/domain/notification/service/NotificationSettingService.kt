package com.gerd.domain.notification.service

import com.gerd.domain.notification.dto.NotificationSettingResponseDTO
import com.gerd.domain.notification.entity.enums.DailyNotificationTime
import com.gerd.domain.notification.entity.enums.NotificationSettingType
import com.gerd.domain.notification.exception.NotificationErrorCode
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.global.apiPayload.GeneralException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class NotificationSettingService(
    private val userNotificationSettingRepository: UserNotificationSettingRepository,
) {

    fun getNotificationSetting(userId: Long): NotificationSettingResponseDTO {
        val setting = userNotificationSettingRepository.findById(userId).orElseThrow {
            GeneralException(NotificationErrorCode.NOTIFICATION_SETTING_NOT_FOUND)
        }
        return NotificationSettingResponseDTO.from(setting)
    }

    @Transactional
    fun toggleNotificationSetting(userId: Long, type: NotificationSettingType) {
        val setting = userNotificationSettingRepository.findById(userId).orElseThrow {
            GeneralException(NotificationErrorCode.NOTIFICATION_SETTING_NOT_FOUND)
        }
        when (type) {
            NotificationSettingType.POST_MEAL -> setting.updatePostMealNotificationEnabled(!setting.postMealNotificationEnabled)
            NotificationSettingType.DAILY_RECORD -> setting.updateDailyRecordNotificationEnabled(!setting.dailyRecordNotificationEnabled)
            NotificationSettingType.WEEKLY_REPORT -> setting.updateWeeklyReportEnabled(!setting.weeklyReportEnabled)
        }
        log.info { "알림 설정 토글: userId=$userId, type=$type" }
    }

    @Transactional
    fun updateDailyNotificationTime(userId: Long, time: DailyNotificationTime) {
        val setting = userNotificationSettingRepository.findById(userId).orElseThrow {
            GeneralException(NotificationErrorCode.NOTIFICATION_SETTING_NOT_FOUND)
        }
        setting.updateDailyNotificationTime(time)
        log.info { "알림 시간대 변경: userId=$userId, time=$time" }
    }
}
