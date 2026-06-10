package com.gerd.domain.onboarding.repository

import com.gerd.domain.food.entity.TriggerLabel
import com.gerd.domain.onboarding.entity.UserTrigger
import com.gerd.domain.onboarding.entity.id.UserTriggerId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserTriggerRepository : JpaRepository<UserTrigger, UserTriggerId> {

    // 판정 입력 조립 시 사용자의 트리거 마스터를 한 번에 로딩 (N+1 방지)
    @Query("select ut.triggerLabel from UserTrigger ut where ut.id.userId = :userId")
    fun findTriggerLabelsByUserId(userId: Long): List<TriggerLabel>
}
