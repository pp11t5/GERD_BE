package com.gerd.domain.admin.entity

import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "admin_action_logs",
    indexes = [Index(name = "idx_admin_action_logs_created_at", columnList = "created_at")],
)
class AdminActionLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_action_log_id")
    val id: Long? = null,

    @Column(name = "admin_email", nullable = false)
    val adminEmail: String,

    @Column(nullable = false, length = 50)
    val action: String,

    @Column(name = "target_type", nullable = false, length = 50)
    val targetType: String,

    @Column(name = "target_id", nullable = false)
    val targetId: String,

    @Column(length = 255)
    val detail: String? = null,
) : BaseTimeEntity()
