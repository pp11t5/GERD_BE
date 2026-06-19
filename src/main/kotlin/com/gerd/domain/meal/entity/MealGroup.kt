package com.gerd.domain.meal.entity

import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime
import java.util.UUID

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
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "eaten_at", nullable = false)
    val eatenAt: LocalDateTime,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),
) : BaseTimeEntity()
