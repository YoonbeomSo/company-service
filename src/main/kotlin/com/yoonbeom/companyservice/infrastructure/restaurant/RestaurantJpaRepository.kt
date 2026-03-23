package com.yoonbeom.companyservice.infrastructure.restaurant

import com.yoonbeom.companyservice.domain.restaurant.Restaurant
import com.yoonbeom.companyservice.domain.restaurant.RestaurantRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RestaurantJpaRepository : JpaRepository<Restaurant, Long>, RestaurantRepository {
    override fun findAllByYearMonth(yearMonth: String): List<Restaurant>
}
