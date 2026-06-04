package com.gerd.domain.food.entity

import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 최근 본 음식 (음식 상세 진입 시 앱이 추가)
 *
 * - (user_id, food_id) 유니크 → 같은 음식 재진입 시 새 row 대신 searchedAt만 갱신(upsert)
 * - 보관 상한(기본 10)은 서비스 레벨에서 강제 (초과분 오래된 것부터 삭제)
 */
@Entity
@Table(
    name = "food_search_history",
    uniqueConstraints = [UniqueConstraint(name = "uk_food_search_history_user_food", columnNames = ["user_id", "food_id"])],
    // 본인 최근순 조회용 — (user_id, searched_at DESC) 복합 인덱스
    indexes = [Index(name = "food_search_history_user_recent_idx", columnList = "user_id, searched_at desc")],
)
class FoodSearchHistory(
    // user는 인증 도메인 소유 — Food.ownerUserId와 동일하게 FK 없이 식별자만 보관
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    val food: Food,

    // 마지막으로 본 시각 — 최근순 정렬·upsert 갱신 대상
    @Column(name = "searched_at", nullable = false)
    var searchedAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_history_id")
    val id: Long? = null,
) : BaseEntity() {

    // 같은 음식 재진입 시 호출 — 조회순서만 최신으로 끌어올린다
    fun touch(at: LocalDateTime) {
        this.searchedAt = at
    }
}
