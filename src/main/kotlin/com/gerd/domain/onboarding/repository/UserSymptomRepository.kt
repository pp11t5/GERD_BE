package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserSymptom
import com.gerd.domain.onboarding.entity.id.UserSymptomId
import org.springframework.data.jpa.repository.JpaRepository

interface UserSymptomRepository : JpaRepository<UserSymptom, UserSymptomId> {

    // 판정 입력 조립 시 사용자의 증상 code 목록 조회 (code는 @EmbeddedId 안에 있어 추가 조인 불필요)
    fun findByIdUserId(userId: Long): List<UserSymptom>
}
