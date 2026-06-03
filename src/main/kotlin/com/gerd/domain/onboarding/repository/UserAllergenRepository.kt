package com.gerd.domain.onboarding.repository

import com.gerd.domain.onboarding.entity.UserAllergen
import com.gerd.domain.onboarding.entity.id.UserAllergenId
import org.springframework.data.jpa.repository.JpaRepository

interface UserAllergenRepository : JpaRepository<UserAllergen, UserAllergenId>
