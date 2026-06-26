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
 * 식사 음식 단일 기록
 */
@Entity
@Table(
    name = "meal_foods",
    indexes = [
        Index(name = "meal_foods_user_eaten_idx", columnList = "user_id, eaten_at"),
        Index(name = "meal_foods_record_idx", columnList = "meal_record_id"),
    ],
)
@SQLDelete(sql = "UPDATE meal_foods SET deleted_at = CURRENT_TIMESTAMP, modified_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class MealFood(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Column(name = "food_id", nullable = false)
    val foodId: Long,

    @Column(name = "meal_record_id", nullable = false)
    val mealRecordId: Long,

    @Column(name = "eaten_at", nullable = false)
    val eatenAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "judged_grade")
    val judgedGrade: JudgmentGrade? = null,

    @Column(name = "analysis_json", columnDefinition = "TEXT")
    var analysisJson: String? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) : BaseEntity() {

}
