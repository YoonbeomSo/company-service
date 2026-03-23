package com.yoonbeom.companyservice.application.restaurant

import com.yoonbeom.companyservice.domain.member.MemberRepository
import com.yoonbeom.companyservice.domain.restaurant.Restaurant
import com.yoonbeom.companyservice.domain.restaurant.RestaurantRepository
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RestaurantService(
    private val restaurantRepository: RestaurantRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun register(
        name: String,
        foodType: String,
        link: String?,
        registrantName: String,
        yearMonth: String,
    ): Restaurant {
        val registrant = memberRepository.findByName(registrantName)
            ?: throw CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다")

        val restaurant = Restaurant.create(
            name = name,
            foodType = foodType,
            link = link,
            registrant = registrant,
            yearMonth = yearMonth,
        )

        return restaurantRepository.save(restaurant)
    }

    @Transactional(readOnly = true)
    fun findByYearMonth(yearMonth: String): List<Restaurant> {
        return restaurantRepository.findAllByYearMonth(yearMonth)
    }
}
