package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserMedication
import org.springframework.data.jpa.repository.JpaRepository

interface UserMedicationRepository : JpaRepository<UserMedication, Long> {

    // 판정 입력 조립 시 사용자의 복용약 목록 조회
    fun findByUserProfileUserId(userId: Long): List<UserMedication>
}
