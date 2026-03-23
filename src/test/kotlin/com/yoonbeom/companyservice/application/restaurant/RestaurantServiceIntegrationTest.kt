package com.yoonbeom.companyservice.application.restaurant

import com.yoonbeom.companyservice.application.auth.AuthService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestaurantServiceIntegrationTest {

    @Autowired
    lateinit var restaurantService: RestaurantService

    @Autowired
    lateinit var authService: AuthService

    @Nested
    @DisplayName("후보지 등록 통합")
    inner class RegisterIntegration {

        @Test
        @DisplayName("후보지를 등록하고 월별 조회에서 확인한다")
        fun registerAndFindByYearMonth() {
            // Arrange
            authService.loginOrRegister("등록자", "1234")

            // Act
            val restaurant = restaurantService.register(
                name = "맛있는 고기집",
                foodType = "한식",
                link = "https://example.com",
                registrantName = "등록자",
                yearMonth = "2026-03",
            )

            // Assert
            assertNotNull(restaurant.id)
            val restaurants = restaurantService.findByYearMonth("2026-03")
            assertEquals(1, restaurants.size)
            assertEquals("맛있는 고기집", restaurants[0].name)
        }

        @Test
        @DisplayName("다른 월의 후보지는 조회되지 않는다")
        fun notFoundInDifferentMonth() {
            // Arrange
            authService.loginOrRegister("등록자", "1234")
            restaurantService.register("맛집", "한식", null, "등록자", "2026-03")

            // Act
            val result = restaurantService.findByYearMonth("2026-04")

            // Assert
            assertEquals(0, result.size)
        }
    }
}
