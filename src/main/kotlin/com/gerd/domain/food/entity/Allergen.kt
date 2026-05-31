package com.gerd.domain.food.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// 알레르기 유발 식품 마스터 (식약처 표시의무 8종 고정 코드) — 시드로 적재되어 food와 M:N으로 연결된다
@Entity
@Table(name = "allergen")
class Allergen(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allergen_id")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column
    val note: String? = null,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,
)
