package com.gerd.domain.onboarding.service

import com.gerd.domain.notification.exception.NotificationErrorCode
import com.gerd.domain.notification.repository.UserNotificationSettingRepository
import com.gerd.domain.onboarding.dto.ConsentRequestDTO
import com.gerd.domain.onboarding.entity.Term
import com.gerd.domain.onboarding.entity.UserConsent
import com.gerd.domain.onboarding.entity.id.UserConsentId
import com.gerd.domain.onboarding.exception.OnboardingErrorCode
import com.gerd.domain.onboarding.repository.TermRepository
import com.gerd.domain.onboarding.repository.UserConsentRepository
import com.gerd.global.apiPayload.GeneralException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ConsentService(
    private val userConsentRepository: UserConsentRepository,
    private val termRepository: TermRepository,
    private val userNotificationSettingRepository: UserNotificationSettingRepository,
) {

    fun getTerms(): List<Term> = termRepository.findLatestAll()

    fun isMarketingAgreed(userId: Long): Boolean =
        userConsentRepository.findLatestByUserIdAndTermCode(userId, MARKETING_TERM_CODE)?.agreed ?: false

    @Transactional
    fun toggleMarketing(userId: Long): Boolean {
        val consent = userConsentRepository.findLatestByUserIdAndTermCode(userId, MARKETING_TERM_CODE)
            ?: throw GeneralException(OnboardingErrorCode.MARKETING_CONSENT_NOT_FOUND)
        consent.updateAgreement(!consent.agreed, LocalDateTime.now())
        if (consent.agreed) {
            enableAllNotifications(userId)
        }

        return consent.agreed
    }

    @Transactional
    fun submitConsent(userId: Long, request: ConsentRequestDTO) {
        val latestTerms = termRepository.findLatestAll()
        val agreedByTermId = request.consents.associateBy { it.termId }

        val allRequiredAgreed = latestTerms
            .filter { it.required }
            .all { agreedByTermId[it.id]?.agreed == true }
        if (!allRequiredAgreed) {
            throw GeneralException(OnboardingErrorCode.REQUIRED_CONSENT_NOT_AGREED)
        }

        val now = LocalDateTime.now()
        val existingByTermId = userConsentRepository.findByIdUserId(userId)
            .associateBy { it.id.termId }

        val consents = latestTerms.map { term ->
            val agreed = agreedByTermId[term.id]?.agreed ?: false
            existingByTermId[term.id]?.apply { updateAgreement(agreed, now) }
                ?: UserConsent(UserConsentId(userId, term.id), term, agreed, now)
        }
        userConsentRepository.saveAll(consents)

        val marketingAgreed = latestTerms
            .firstOrNull { it.code == MARKETING_TERM_CODE }
            ?.let { agreedByTermId[it.id]?.agreed == true }
            ?: false
        if (marketingAgreed) {
            enableAllNotifications(userId)
        }
    }

    // 마케팅 동의 시 알림 설정을 전부 활성화 — 가입 시 알림 설정이 함께 생성되므로 항상 존재해야 정상
    private fun enableAllNotifications(userId: Long) {
        userNotificationSettingRepository.findById(userId)
            .orElseThrow { GeneralException(NotificationErrorCode.NOTIFICATION_SETTING_NOT_FOUND) }
            .enableAll()
    }

    companion object {
        private const val MARKETING_TERM_CODE = "marketing"
    }
}
