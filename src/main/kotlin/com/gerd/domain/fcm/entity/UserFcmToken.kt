package com.gerd.domain.fcm.entity

import com.gerd.domain.auth.entity.User
import com.gerd.domain.fcm.entity.enums.DevicePlatform
import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "user_fcm_tokens")
class UserFcmToken(

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    // @MapsId가 채우는 공유 PK, 직접 할당 X
    @Id
    @Column(name = "user_id")
    val userId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var platform: DevicePlatform,

    // FCM 토큰은 길이가 길 수 있으므로 충분히 큰 길이로 설정
    @Column(nullable = false, length = 1000)
    var token: String,

) : BaseTimeEntity() {

    fun updateToken(newToken: String, newPlatform: DevicePlatform) {
        token = newToken
        platform = newPlatform
    }
}
