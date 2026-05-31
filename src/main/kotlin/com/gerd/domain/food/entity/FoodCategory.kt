package com.gerd.domain.food.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// 음식 분류 마스터 (밥·죽, 면류 등 13종 고정 코드) — 시드로 적재되어 food와 M:N으로 연결된다
@Entity
@Table(name = "food_category")
class FoodCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_category_id")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,
)
