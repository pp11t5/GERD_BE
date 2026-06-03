package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserTrigger
import com.gerd.domain.onboarding.entity.id.UserTriggerId
import org.springframework.data.jpa.repository.JpaRepository

interface UserTriggerRepository : JpaRepository<UserTrigger, UserTriggerId>
