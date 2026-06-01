package com.gerd.global.common.entity

import com.fasterxml.jackson.annotation.JsonFormat
import com.querydsl.core.annotations.QueryExclude
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UuidGenerator
import org.hibernate.annotations.UuidGenerator.Style
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@MappedSuperclass
@QueryExclude
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @UuidGenerator(style = Style.TIME)
    @Column(name = "external_id", columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
    var externalId: UUID? = null

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    var createdAt: LocalDateTime? = null
        protected set

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    var modifiedAt: LocalDateTime? = null
        protected set
}
