package com.gerd.domain.auth.entity

import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.domain.auth.entity.enums.UserStatus
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

    @Column
    var nickname: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "profile_image")
    var profileImage: String? = null,

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isDeleted(): Boolean = deletedAt != null

    fun updateLastLoginAt() {
        lastLoginAt = LocalDateTime.now()
    }

    // 탈퇴 요청 — deleted_at 기록으로 14일 유예 시작
    fun withdraw() {
        status = UserStatus.DELETED
        deletedAt = LocalDateTime.now()
    }

    // 탈퇴 유예기간 중 복구 — ACTIVE로 상태 초기화
    fun recover() {
        status = UserStatus.ACTIVE
        deletedAt = null
        lastLoginAt = LocalDateTime.now()
    }
}
