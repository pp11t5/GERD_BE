package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserSymptom
import com.gerd.domain.onboarding.entity.id.UserSymptomId
import org.springframework.data.jpa.repository.JpaRepository

interface UserSymptomRepository : JpaRepository<UserSymptom, UserSymptomId>
