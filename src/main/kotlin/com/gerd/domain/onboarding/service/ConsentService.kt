package com.gerd.domain.onboarding.service

import com.gerd.domain.auth.repository.UserRepository
import com.gerd.domain.notification.entity.UserNotificationSetting
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.enums.ConsentType
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.UserConsentRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 약관동의 처리 서비스
 *
 * - 필수 3종 동의 검증
 * - type별 현재 상태 upsert (재호출 시 동의 상태·시점 갱신)
 */
@Service
@Transactional(readOnly = true)
class ConsentService(
    private val userConsentRepository: UserConsentRepository,
    private val userNotificationSettingRepository: UserNotificationSettingRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun submitConsent(userId: Long, request: ConsentRequestDTO) {
        // 필수 3종 미동의 시 온보딩 진입 거부
        if (!request.tos || !request.privacy || !request.healthSensitive) {
            throw GeneralException(OnboardingErrorCode.REQUIRED_CONSENT_NOT_AGREED)
        }

        val now = LocalDateTime.now()
        val agreedByType = mapOf(
            ConsentType.TOS to request.tos,
            ConsentType.PRIVACY to request.privacy,
            ConsentType.HEALTH_SENSITIVE to request.healthSensitive,
            ConsentType.MARKETING to request.marketing,
        )

        // 마케팅·푸시 동의 여부를 알림 설정에 반영 — 동의 시 알림 켜짐, 미동의 시 꺼짐
        val enabled = request.marketing
        val setting = userNotificationSettingRepository.findById(userId).orElse(null)
        if (setting == null) {
            // @MapsId 공유 PK — getReferenceById로 SELECT 없이 FK만 확정
            userNotificationSettingRepository.save(
                UserNotificationSetting(
                    user = userRepository.getReferenceById(userId),
                    postMealNotificationEnabled = enabled,
                    dailyRecordNotificationEnabled = enabled,
                    weeklyReportEnabled = enabled,
                ),
            )
        } else {
            setting.update(
                postMealNotificationEnabled = enabled,
                dailyRecordNotificationEnabled = enabled,
                dailyNotificationTime = setting.dailyNotificationTime,
                weeklyReportEnabled = enabled,
            )
        }

        val existingByCode = userConsentRepository.findByIdUserId(userId)
            .associateBy { it.id.consentType }

        val consents = agreedByType.map { (type, agreed) ->
            existingByCode[type.code]?.apply { updateAgreement(agreed, now) }
                ?: UserConsent(UserConsentId(userId, type.code), agreed, now)
        }
        userConsentRepository.saveAll(consents)
    }
}
