package com.gerd.global.common.entity

import com.querydsl.core.annotations.QueryExclude
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UuidGenerator
import org.hibernate.annotations.UuidGenerator.Style
import java.util.UUID

@MappedSuperclass
@QueryExclude
abstract class BaseEntity : BaseTimeEntity() {

    @UuidGenerator(style = Style.TIME)
    @Column(name = "external_id", updatable = false, nullable = false, unique = true)
    var externalId: UUID? = null
}
