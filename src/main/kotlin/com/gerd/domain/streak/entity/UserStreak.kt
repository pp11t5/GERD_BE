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
import jakarta.persistence.Version
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
    lastComfortableDate: LocalDate? = null,
) : BaseTimeEntity() {

    @Column(name = "current_streak", nullable = false)
    var currentStreak: Int = currentStreak
        protected set

    @Column(name = "last_comfortable_date")
    var lastComfortableDate: LocalDate? = lastComfortableDate
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L
        protected set

    fun recordComfortableOn(recordDate: LocalDate) {
        val lastDate = lastComfortableDate
        if (lastDate == recordDate) {
            return
        }

        if (lastDate == null) {
            currentStreak = 1
            lastComfortableDate = recordDate
            return
        }

        if (recordDate.isBefore(lastDate)) {
            val earliestDate = lastDate.minusDays(currentStreak.toLong() - 1)
            if (currentStreak > 0 && recordDate == earliestDate.minusDays(1)) {
                currentStreak += 1
            }
            return
        }

        currentStreak = if (recordDate == lastDate.plusDays(1)) currentStreak + 1 else 1
        lastComfortableDate = recordDate
    }

    fun replace(currentStreak: Int, lastComfortableDate: LocalDate?) {
        this.currentStreak = currentStreak
        this.lastComfortableDate = lastComfortableDate
    }
}
