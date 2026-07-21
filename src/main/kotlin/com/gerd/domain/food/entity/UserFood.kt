package com.gerd.domain.food.entity

import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

// 관리자가 특정 사용자에게 등록한 음식 매핑
// is_unknown: food.source = USER → true (판정 근거 없음), SEED/CURATED → false
@Entity
@Table(
    name = "user_foods",
    uniqueConstraints = [UniqueConstraint(name = "uq_user_foods_user_food", columnNames = ["user_id", "food_id"])],
)
class UserFood(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_food_id")
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    val food: Food,

    @Column(name = "is_unknown", nullable = false)
    val isUnknown: Boolean,
) : BaseTimeEntity()
