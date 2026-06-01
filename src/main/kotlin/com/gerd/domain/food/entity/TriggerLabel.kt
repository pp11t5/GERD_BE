package com.gerd.domain.food.entity

import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// 역류 유발 요인 마스터 (커피, 매운 음식 등 고정 코드 집합) — 시드로 적재되어 food와 M:N으로 연결된다
@Entity
@Table(name = "trigger_labels")
class TriggerLabel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trigger_label_id")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column
    val description: String? = null,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,
) : BaseEntity()
