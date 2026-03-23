package com.yoonbeom.companyservice.domain.restaurant

import com.yoonbeom.companyservice.domain.member.Member
import com.yoonbeom.companyservice.support.util.YearMonthUtils
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "restaurant")
class Restaurant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "food_type", nullable = false, length = 50)
    var foodType: String,

    @Column(length = 500)
    var link: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id", nullable = false)
    var registrant: Member,

    @Column(name = "target_month", nullable = false, length = 7)
    var yearMonth: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun create(
            name: String,
            foodType: String,
            link: String?,
            registrant: Member,
            yearMonth: String,
        ): Restaurant {
            require(name.isNotBlank()) { "가게이름은 비어있을 수 없습니다" }
            require(foodType.isNotBlank()) { "음식종류는 비어있을 수 없습니다" }
            require(yearMonth.matches(YearMonthUtils.YEAR_MONTH_PATTERN)) { "yearMonth 형식은 YYYY-MM이어야 합니다" }

            return Restaurant(
                name = name,
                foodType = foodType,
                link = link?.takeIf { it.isNotBlank() },
                registrant = registrant,
                yearMonth = yearMonth,
            )
        }
    }
}
