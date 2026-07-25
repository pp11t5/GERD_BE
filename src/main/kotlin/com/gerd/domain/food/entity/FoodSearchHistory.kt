package com.gerd.domain.food.entity

import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 최근 검색어 (음식 검색 API 호출 시 자동 추가)
 *
 * - (user_id, query) 유니크 → 같은 검색어 재검색 시 새 row 대신 searchedAt만 갱신(upsert)
 * - 보관 상한(기본 10)은 서비스 레벨에서 강제 (초과분 오래된 것부터 삭제)
 */
@Entity
@Table(
    name = "food_search_history",
    uniqueConstraints = [UniqueConstraint(name = "uk_food_search_history_user_query", columnNames = ["user_id", "query"])],
    // 본인 최근순 조회용 — (user_id, searched_at DESC) 복합 인덱스
    indexes = [Index(name = "food_search_history_user_recent_idx", columnList = "user_id, searched_at desc")],
)
class FoodSearchHistory(
    // user는 인증 도메인 소유 — Food.ownerUserId와 동일하게 FK 없이 식별자만 보관
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    // 검색 API에 실제로 입력된 검색어(NFC 정규화·trim 적용된 값) — FoodSearchService.MAX_QUERY_LENGTH와 길이 일치
    @Column(name = "query", nullable = false, length = 100)
    val query: String,

    // 마지막으로 검색한 시각 — 최근순 정렬·upsert 갱신 대상
    @Column(name = "searched_at", nullable = false)
    var searchedAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_history_id")
    val id: Long? = null,
) : BaseEntity() {

    // 같은 검색어 재검색 시 호출 — 조회순서만 최신으로 끌어올린다
    fun touch(at: LocalDateTime) {
        this.searchedAt = at
    }
}
