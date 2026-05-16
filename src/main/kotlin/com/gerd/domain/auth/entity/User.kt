package com.gerd.domain.auth.entity

import com.gerd.domain.auth.entity.enums.UserRole
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var password: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,
) : BaseEntity() {

    fun changeRole(role: UserRole) {
        this.role = role
    }

    fun encodePassword(password: String?) {
        this.password = requireNotNull(password) { "Password must not be null." }
    }
}
