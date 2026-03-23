package com.yoonbeom.companyservice.application.restaurant

import com.yoonbeom.companyservice.domain.member.Member
import com.yoonbeom.companyservice.domain.member.MemberRepository
import com.yoonbeom.companyservice.domain.restaurant.Restaurant
import com.yoonbeom.companyservice.domain.restaurant.RestaurantRepository
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class RestaurantServiceTest {

    @Mock
    lateinit var restaurantRepository: RestaurantRepository

    @Mock
    lateinit var memberRepository: MemberRepository

    @InjectMocks
    lateinit var restaurantService: RestaurantService

    private fun createMember(id: Long = 1L): Member {
        val member = Member.create("테스트유저", "\$2a\$10\$hash")
        member.id = id
        return member
    }

    @Nested
    @DisplayName("후보지 등록")
    inner class Register {

        @Test
        @DisplayName("유효한 정보로 후보지를 등록한다")
        fun registerRestaurant_whenValidInput() {
            // Arrange
            val memberId = 1L
            val member = createMember(memberId)
            val savedRestaurant = Restaurant.create("맛있는 집", "한식", null, member, "2026-03")

            whenever(memberRepository.findByName("테스트유저")).thenReturn(member)
            whenever(restaurantRepository.save(any())).thenReturn(savedRestaurant)

            // Act
            val result = restaurantService.register(
                name = "맛있는 집",
                foodType = "한식",
                link = null,
                registrantName = "테스트유저",
                yearMonth = "2026-03",
            )

            // Assert
            assertEquals("맛있는 집", result.name)
            assertEquals("한식", result.foodType)
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        fun throwException_whenMemberNotFound() {
            // Arrange
            whenever(memberRepository.findByName("없는유저")).thenReturn(null)

            // Act & Assert
            assertThrows<CoreException> {
                restaurantService.register("맛집", "한식", null, "없는유저", "2026-03")
            }
        }
    }

    @Nested
    @DisplayName("월별 후보지 조회")
    inner class FindByYearMonth {

        @Test
        @DisplayName("해당 월의 후보지 목록을 반환한다")
        fun returnRestaurants_whenYearMonthGiven() {
            // Arrange
            val member = createMember()
            val restaurants = listOf(
                Restaurant.create("한식당", "한식", null, member, "2026-03"),
                Restaurant.create("일식당", "일식", "https://link.com", member, "2026-03"),
            )
            whenever(restaurantRepository.findAllByYearMonth("2026-03")).thenReturn(restaurants)

            // Act
            val result = restaurantService.findByYearMonth("2026-03")

            // Assert
            assertEquals(2, result.size)
        }

        @Test
        @DisplayName("해당 월에 후보지가 없으면 빈 목록을 반환한다")
        fun returnEmptyList_whenNoRestaurants() {
            // Arrange
            whenever(restaurantRepository.findAllByYearMonth("2026-04")).thenReturn(emptyList())

            // Act
            val result = restaurantService.findByYearMonth("2026-04")

            // Assert
            assertEquals(0, result.size)
        }
    }
}
