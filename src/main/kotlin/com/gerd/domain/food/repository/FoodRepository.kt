package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.Food
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FoodRepository : JpaRepository<Food, Long>, FoodRepositoryCustom {

    // 최근 본 음식 추가 시 externalId로 음식 resolve (soft-deleted는 @SQLRestriction으로 자동 제외)
    fun findByExternalId(externalId: UUID): Food?
}
