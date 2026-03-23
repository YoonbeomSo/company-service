package com.yoonbeom.companyservice.domain.restaurant

import com.yoonbeom.companyservice.domain.member.Member
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RestaurantTest {

    private fun createMember(): Member = Member.create("홍길동", "\$2a\$10\$hash")

    @Nested
    @DisplayName("후보지 생성")
    inner class Create {

        @Test
        @DisplayName("필수 정보로 후보지를 생성한다")
        fun createRestaurant_whenValidInput() {
            // Arrange
            val registrant = createMember()
            val name = "맛있는 고기집"
            val foodType = "한식"
            val yearMonth = "2026-03"

            // Act
            val restaurant = Restaurant.create(
                name = name,
                foodType = foodType,
                link = null,
                registrant = registrant,
                yearMonth = yearMonth,
            )

            // Assert
            assertEquals(name, restaurant.name)
            assertEquals(foodType, restaurant.foodType)
            assertNull(restaurant.link)
            assertEquals(registrant, restaurant.registrant)
            assertEquals(yearMonth, restaurant.yearMonth)
            assertNotNull(restaurant.createdAt)
        }

        @Test
        @DisplayName("링크를 포함하여 후보지를 생성한다")
        fun createRestaurant_withLink() {
            // Arrange
            val registrant = createMember()

            // Act
            val restaurant = Restaurant.create(
                name = "스시 오마카세",
                foodType = "일식",
                link = "https://naver.me/example",
                registrant = registrant,
                yearMonth = "2026-03",
            )

            // Assert
            assertEquals("https://naver.me/example", restaurant.link)
        }

        @Test
        @DisplayName("가게이름이 빈 문자열이면 예외를 던진다")
        fun throwException_whenNameIsBlank() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Restaurant.create(
                    name = "",
                    foodType = "한식",
                    link = null,
                    registrant = createMember(),
                    yearMonth = "2026-03",
                )
            }
        }

        @Test
        @DisplayName("음식종류가 빈 문자열이면 예외를 던진다")
        fun throwException_whenFoodTypeIsBlank() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Restaurant.create(
                    name = "맛있는 집",
                    foodType = "",
                    link = null,
                    registrant = createMember(),
                    yearMonth = "2026-03",
                )
            }
        }

        @Test
        @DisplayName("yearMonth 형식이 잘못되면 예외를 던진다")
        fun throwException_whenYearMonthInvalid() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Restaurant.create(
                    name = "맛있는 집",
                    foodType = "한식",
                    link = null,
                    registrant = createMember(),
                    yearMonth = "2026-3",
                )
            }
        }
    }
}
