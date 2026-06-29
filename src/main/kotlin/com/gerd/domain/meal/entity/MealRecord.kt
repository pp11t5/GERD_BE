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

/**
 * 식사 그룹 기록
 */
@Entity
@Table(
    name = "meal_records",
    indexes = [Index(name = "meal_records_user_idx", columnList = "user_id")],
)
@SQLDelete(sql = "UPDATE meal_records SET deleted_at = CURRENT_TIMESTAMP, modified_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class MealRecord(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Column(name = "eaten_at", nullable = false)
    val eatenAt: LocalDateTime,

    grade: JudgmentGrade? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "grade")
    var grade: JudgmentGrade? = grade
        protected set

    // 신규 끼니 첫 음식 등록 시
    fun initGrade(foodGrade: JudgmentGrade) {
        grade = foodGrade
    }

    // 같이 먹은 음식 추가 시 — 더 나쁜 등급이면 교체
    fun updateGrade(foodGrade: JudgmentGrade) {
        val current = grade
        if (current == null || foodGrade.priority < current.priority) {
            grade = foodGrade
        }
    }

    // 음식 삭제 후 남은 등급 전체로 재산정
    fun recalculateGrade(remainingGrades: List<JudgmentGrade>) {
        grade = remainingGrades.minByOrNull { it.priority }
    }
}
