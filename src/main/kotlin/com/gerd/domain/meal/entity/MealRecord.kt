package com.gerd.domain.meal.entity

import com.gerd.domain.auth.entity.User
import com.gerd.domain.judgment.dto.enums.JudgmentGrade
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime
import java.util.UUID

/**
 * 식사 기록 (음식 1건 단위)
 *
 * - 단일 테이블 + meal_group_id 묶음 키로 끼니를 표현 (ADR-0016) — 별도 그룹 테이블 없음
 * - judgedGrade: FE가 신호등 판정 화면에서 들고 온 등급 스냅샷 (ADR-0017) — 기록 시점 사실 보존, 이후 갱신 안 함
 * - food는 음식 도메인 소유 + soft-delete 후에도 기록이 음식 정보를 보존해야 해(D5)
 *   @SQLRestriction이 걸린 연관 매핑 대신 식별자(foodId)만 보관한다
 * - deletedAt 기반 soft delete — 삭제 시 UPDATE로 전환되고 조회 시 활성 row만 노출
 */
@Entity
@Table(
    name = "meal_records",
    indexes = [
        Index(name = "meal_records_user_eaten_idx", columnList = "user_id, eaten_at"),
        Index(name = "meal_records_group_idx", columnList = "meal_group_id"),
    ],
)
@SQLDelete(sql = "UPDATE meal_records SET deleted_at = CURRENT_TIMESTAMP, modified_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class MealRecord(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Column(name = "food_id", nullable = false)
    val foodId: Long,

    // 끼니 묶음 키 — 단독 생성은 새 uuid, "같이 먹은 음식" 추가는 기존 끼니의 키 지정 (ADR-0016)
    @Column(name = "meal_group_id", nullable = false)
    val mealGroupId: UUID,

    @Column(name = "eaten_at", nullable = false)
    val eatenAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "judged_grade")
    val judgedGrade: JudgmentGrade? = null,

    @Column(length = 200)
    var memo: String? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) : BaseEntity() {

    // 수정 화면의 유일한 편집 필드 — null/공백 문자열은 메모 삭제로 취급 (D7)
    fun updateMemo(memo: String?) {
        this.memo = memo?.trim()?.takeUnless { it.isEmpty() }
    }
}
