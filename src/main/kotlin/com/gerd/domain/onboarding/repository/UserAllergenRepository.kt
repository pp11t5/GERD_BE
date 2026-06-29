package com.gerd.domain.onboarding.repository

import com.gerd.domain.food.entity.Allergen
import com.gerd.domain.onboarding.entity.UserAllergen
import com.gerd.domain.onboarding.entity.id.UserAllergenId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserAllergenRepository : JpaRepository<UserAllergen, UserAllergenId> {

    // 판정 입력 조립 시 사용자의 알레르겐 마스터를 한 번에 로딩 (N+1 방지)
    @Query("select ua.allergen from UserAllergen ua where ua.id.userId = :userId")
    fun findAllergensByUserId(userId: Long): List<Allergen>

    fun deleteAllByUserProfileUserId(userId: Long)
}
