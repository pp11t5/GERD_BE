package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.Food
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FoodRepository : JpaRepository<Food, Long>, FoodRepositoryCustom {

    // 최근 본 음식 추가 시 externalId로 음식 resolve (soft-deleted는 @SQLRestriction으로 자동 제외)
    fun findByExternalId(externalId: UUID): Food?

    // 식사 기록 조회용 — 삭제된 음식도 포함 (D5: food soft-delete 후에도 기록의 음식 정보 보존).
    // @SQLRestriction은 전역 필터라 네이티브 쿼리로 우회한다 (Food.kt 주석 참고)
    @Query(value = "SELECT * FROM foods WHERE food_id IN (:ids)", nativeQuery = true)
    fun findAllByIdsIncludingDeleted(@Param("ids") ids: Collection<Long>): List<Food>
}
