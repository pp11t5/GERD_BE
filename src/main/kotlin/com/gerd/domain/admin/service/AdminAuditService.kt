package com.gerd.domain.admin.service

import com.gerd.domain.admin.entity.AdminActionLog
import com.gerd.domain.admin.repository.AdminActionLogRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AdminAuditService(
    private val adminActionLogRepository: AdminActionLogRepository,
) {

    fun record(action: String, targetType: String, targetId: String, detail: String? = null) {
        val adminName = SecurityContextHolder.getContext().authentication?.name ?: UNKNOWN_ADMIN
        adminActionLogRepository.save(
            AdminActionLog(
                adminEmail = adminName,
                action = action,
                targetType = targetType,
                targetId = targetId,
                detail = detail?.take(DETAIL_MAX_LENGTH),
            ),
        )
    }

    companion object {
        const val FOOD_PROMOTE = "FOOD_PROMOTE"
        const val TARGET_FOOD = "FOOD"
        private const val UNKNOWN_ADMIN = "unknown"
        private const val DETAIL_MAX_LENGTH = 255
    }
}
