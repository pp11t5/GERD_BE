package com.gerd.domain.auth.entity

import com.gerd.domain.auth.entity.enums.AuthProvider
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
    name = "auth_accounts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_auth_accounts_provider_provider_account_id",
            columnNames = ["provider", "provider_account_id"],
        ),
    ],
)
class AuthAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_account_id")
    val id: Long? = null,

    // @SQLRestriction 우회용 — user lazy load 없이 FK 값 직접 접근
    @Column(name = "user_id", insertable = false, updatable = false, nullable = false)
    val userId: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: AuthProvider,

    @Column(name = "provider_account_id", nullable = false, length = 100)
    val providerAccountId: String,
) : BaseEntity()
