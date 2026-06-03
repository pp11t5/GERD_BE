package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.FoodSearchHistory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FoodSearchHistoryRepository : JpaRepository<FoodSearchHistory, Long> {

    // upsert 판정 — (user_id, food_id) 기존 항목 조회
    fun findByUserIdAndFoodId(userId: Long, foodId: Long): FoodSearchHistory?

    // 단건 삭제 시 소유권 확인 — 본인 항목만 삭제 가능
    fun findByIdAndUserId(id: Long, userId: Long): FoodSearchHistory?

    fun deleteByUserId(userId: Long)

    /**
     * 본인 최근순 조회 — soft-deleted 음식은 제외하고 food를 함께 fetch(N+1 방지).
     * food는 단일 연관이라 fetch join + Pageable이 안전하다.
     * searchedAt 동률 시 순서가 흔들리지 않도록 id desc를 tie-breaker로 둔다.
     */
    @Query(
        """
        select h from FoodSearchHistory h
        join fetch h.food f
        where h.userId = :userId and f.deletedAt is null
        order by h.searchedAt desc, h.id desc
        """,
    )
    fun findRecentWithFood(userId: Long, pageable: Pageable): List<FoodSearchHistory>

    // 보관 상한 정리용 — 최근순 전체 id (상한 초과분 삭제 대상 선별)
    // searchedAt 동률 시 삭제 대상이 비결정적이지 않도록 id desc를 tie-breaker로 둔다.
    @Query("select h.id from FoodSearchHistory h where h.userId = :userId order by h.searchedAt desc, h.id desc")
    fun findIdsByUserIdOrderByRecent(userId: Long): List<Long>
}
