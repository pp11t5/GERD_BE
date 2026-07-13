package com.gerd.domain.onboarding.entity

import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "terms",
    uniqueConstraints = [UniqueConstraint(name = "uq_terms_code_effective_date", columnNames = ["code", "effective_date"])],
)
class Term(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 20)
    val version: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false)
    val required: Boolean,

    @Column(name = "effective_date", nullable = false)
    val effectiveDate: LocalDate,
) : BaseTimeEntity()
