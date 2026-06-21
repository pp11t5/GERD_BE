package com.gerd.domain.dictionary.entity

import com.gerd.domain.auth.entity.User
import com.gerd.domain.dictionary.entity.enums.DictionaryType
import com.gerd.domain.food.entity.Food
import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
    name = "user_food_dictionaries",
    indexes = [
        // 커서 페이징(user_id + type 필터, id desc 정렬) 및 user_id 단독 카운트를 함께 커버.
        Index(name = "user_food_dict_user_type_id_idx", columnList = "user_id, dictionary_type, id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_user_food_dict", columnNames = ["user_id", "food_id", "dictionary_type"]),
    ],
)
class UserFoodDictionary(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    val food: Food,

    @Enumerated(EnumType.STRING)
    @Column(name = "dictionary_type", nullable = false, length = 10)
    val dictionaryType: DictionaryType,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) : BaseTimeEntity()
