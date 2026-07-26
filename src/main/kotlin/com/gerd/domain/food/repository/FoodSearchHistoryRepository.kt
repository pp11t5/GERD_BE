package com.gerd.domain.food.repository

import com.gerd.domain.food.entity.FoodSearchHistory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FoodSearchHistoryRepository : JpaRepository<FoodSearchHistory, Long> {

    interface TopSearchedFoodView {
        fun getExternalId(): String
        fun getName(): String
        fun getCategory(): String?
    }

    // upsert 판정 — (user_id, query) 기존 항목 조회
    fun findByUserIdAndQuery(userId: Long, query: String): FoodSearchHistory?

    // 단건 삭제 시 소유권 확인 — 본인 항목만 id로 조회
    fun findByIdAndUserId(id: Long, userId: Long): FoodSearchHistory?

    fun deleteByUserId(userId: Long)

    // 본인 최근순 조회. searchedAt 동률 시 순서가 흔들리지 않도록 id desc를 tie-breaker로 둔다.
    @Query("select h from FoodSearchHistory h where h.userId = :userId order by h.searchedAt desc, h.id desc")
    fun findRecentByUserId(userId: Long, pageable: Pageable): List<FoodSearchHistory>

    // 보관 상한 정리용 — 최근순 전체 id (상한 초과분 삭제 대상 선별)
    // searchedAt 동률 시 삭제 대상이 비결정적이지 않도록 id desc를 tie-breaker로 둔다.
    @Query("select h.id from FoodSearchHistory h where h.userId = :userId order by h.searchedAt desc, h.id desc")
    fun findIdsByUserIdOrderByRecent(userId: Long): List<Long>

    // 전체 유저 검색 기록 기준 가장 많이 검색된 공개 음식 상위 3개 — food name과 query 정규화 일치
    @Query(
        value = """
            SELECT f.external_id::text AS external_id, f.name AS name,
                   (SELECT fc.code FROM food_category_maps fcm
                    JOIN food_categories fc ON fc.food_category_id = fcm.food_category_id
                    WHERE fcm.food_id = f.food_id
                    ORDER BY fc.sort_order
                    LIMIT 1) AS category
            FROM food_search_histories fsh
            JOIN foods f
              ON LOWER(REPLACE(f.name, ' ', '')) = LOWER(REPLACE(fsh.query, ' ', ''))
             AND f.deleted_at IS NULL
             AND f.visibility = 'public'
            GROUP BY f.food_id, f.external_id, f.name
            ORDER BY COUNT(*) DESC
            LIMIT 3
        """,
        nativeQuery = true,
    )
    fun findTop3MostSearched(): List<TopSearchedFoodView>
}
