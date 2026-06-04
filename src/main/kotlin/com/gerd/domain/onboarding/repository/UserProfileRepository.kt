package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserProfile
import org.springframework.data.jpa.repository.JpaRepository

interface UserProfileRepository : JpaRepository<UserProfile, Long>
