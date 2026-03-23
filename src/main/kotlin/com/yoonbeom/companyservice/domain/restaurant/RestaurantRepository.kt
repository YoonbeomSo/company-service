package com.yoonbeom.companyservice.domain.restaurant

interface RestaurantRepository {
    fun save(restaurant: Restaurant): Restaurant
    fun findAllByYearMonth(yearMonth: String): List<Restaurant>
    fun findAllById(ids: Iterable<Long>): List<Restaurant>
}
