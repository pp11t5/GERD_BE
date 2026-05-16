package com.project.<domain>.entity

import com.project.global.common.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "<plural_table_name>")
class ExampleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "example_id")
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: OwnerEntity,
) : BaseEntity() {

    @OneToMany(
        mappedBy = "example",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE],
        orphanRemoval = false,
    )
    val items: MutableList<ExampleItemEntity> = mutableListOf()

    fun changeDescription(description: String?) {
        this.description = description
    }

    fun addItem(item: ExampleItemEntity) {
        items.add(item)
        item.connectExample(this)
    }

    fun removeItem(item: ExampleItemEntity) {
        items.remove(item)
        item.disconnectExample()
    }

    companion object {
        // 생성 시 강제할 규칙(파생값, 기본 상태, 검증, 생성 경로 제한)이 있을 때만 create()를 둔다.
        fun create(
            name: String,
            description: String?,
            owner: OwnerEntity,
        ): ExampleEntity = ExampleEntity(
            name = name.trim(),
            description = description,
            owner = owner,
        )
    }
}
