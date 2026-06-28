package com.gerd.domain.streak.entity

import com.gerd.domain.auth.entity.User
import com.gerd.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDate

@Entity
@Table(name = "user_streaks")
class UserStreak(

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Id
    @Column(name = "user_id")
    val userId: Long? = null,

    currentStreak: Int = 0,
    lastRecordDate: LocalDate? = null,
) : BaseTimeEntity() {

    @Column(name = "current_streak", nullable = false)
    var currentStreak: Int = currentStreak
        protected set

    @Column(name = "last_record_date")
    var lastRecordDate: LocalDate? = lastRecordDate
        protected set

    fun recordOn(recordDate: LocalDate) {
        if (lastRecordDate == recordDate) {
            return
        }

        currentStreak = if (lastRecordDate == recordDate.minusDays(1)) {
            currentStreak + 1
        } else {
            1
        }
        lastRecordDate = recordDate
    }

    fun streakCountOn(date: LocalDate): Int =
        if (lastRecordDate == date || lastRecordDate == date.minusDays(1)) {
            currentStreak
        } else {
            0
        }

    fun replace(streak: Int, lastRecordDate: LocalDate?) {
        this.currentStreak = streak
        this.lastRecordDate = lastRecordDate
    }
}
