package com.gerd.domain.food.entity

import com.gerd.domain.food.converter.FoodSourceConverter
import com.gerd.domain.food.converter.FoodVisibilityConverter
import com.gerd.domain.food.entity.enums.FoodSource
import com.gerd.domain.food.entity.enums.FoodVisibility
import com.gerd.global.common.entity.BaseEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

/**
 * 음식 카탈로그 (메뉴 단위)
 *
 * - seed/curated/user 음식을 source/visibility로 구분해 단일 테이블에 통합
 * - deletedAt 기반 soft delete — delete 시 UPDATE로 전환되고 조회 시 활성 row만 노출
 */
// 스키마 명세(food-schema.sql)가 updated_at을 SoT로 두므로 BaseEntity.modifiedAt 컬럼을 updated_at으로 매핑한다
// @SQLRestriction은 전역 필터라 삭제 row 조회가 필요하면 네이티브 쿼리로 우회한다
@Entity
@Table(name = "foods")
@AttributeOverride(name = "modifiedAt", column = Column(name = "updated_at"))
@SQLDelete(sql = "UPDATE foods SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE food_id = ?")
@SQLRestriction("deleted_at IS NULL")
class Food(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_id")
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Convert(converter = FoodSourceConverter::class)
    @Column(nullable = false)
    val source: FoodSource,

    @Convert(converter = FoodVisibilityConverter::class)
    @Column(nullable = false)
    val visibility: FoodVisibility,

    // user food일 때만 채워지는 소유자 참조 — 도메인 경계가 달라 FK 없이 식별자만 보관한다
    @Column(name = "owner_user_id")
    val ownerUserId: Long? = null,

    @Column(name = "image_url")
    var imageUrl: String? = null,

    @Column
    var description: String? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {

    fun updateDetails(name: String, description: String?, imageUrl: String?) {
        this.name = name
        this.description = description
        this.imageUrl = imageUrl
    }
}
