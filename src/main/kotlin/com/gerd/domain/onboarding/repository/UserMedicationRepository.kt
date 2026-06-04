package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserMedication
import org.springframework.data.jpa.repository.JpaRepository

interface UserMedicationRepository : JpaRepository<UserMedication, Long>
