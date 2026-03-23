package com.yoonbeom.companyservice.application.vote

import com.yoonbeom.companyservice.application.auth.AuthService
import com.yoonbeom.companyservice.application.restaurant.RestaurantService
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VoteServiceIntegrationTest {

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var voteService: VoteService

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var restaurantService: RestaurantService

    @Nested
    @DisplayName("투표 생성 통합")
    inner class CreateVoteIntegration {

        @Test
        @DisplayName("투표 생성 후 해당 월의 후보지만 VoteItem으로 포함된다")
        fun containOnlyCurrentMonthRestaurants_whenVoteCreated() {
            // Arrange
            authService.loginOrRegister("생성자", "1234")
            restaurantService.register("3월맛집A", "한식", null, "생성자", "2026-03")
            restaurantService.register("3월맛집B", "일식", null, "생성자", "2026-03")
            restaurantService.register("4월맛집", "중식", null, "생성자", "2026-04")

            // Act
            val vote = voteService.createVote(
                title = "3월 회식 투표",
                yearMonth = "2026-03",
                maxSelections = 1,
                deadline = LocalDateTime.of(2026, 3, 31, 18, 0),
                creatorName = "생성자",
            )

            // Assert
            assertNotNull(vote.id)
            assertEquals(2, vote.voteItems.size)
            val itemNames = vote.voteItems.map { it.restaurant.name }
            assertTrue(itemNames.contains("3월맛집A"))
            assertTrue(itemNames.contains("3월맛집B"))
        }

        @Test
        @DisplayName("투표 생성 후 추가된 후보지는 해당 투표에 포함되지 않는다")
        fun notContainLaterAddedRestaurant_whenVoteAlreadyCreated() {
            // Arrange
            authService.loginOrRegister("생성자", "1234")
            restaurantService.register("기존맛집", "한식", null, "생성자", "2026-03")

            val vote = voteService.createVote(
                title = "3월 회식",
                yearMonth = "2026-03",
                maxSelections = 1,
                deadline = LocalDateTime.of(2026, 3, 31, 18, 0),
                creatorName = "생성자",
            )

            // Act
            restaurantService.register("나중맛집", "일식", null, "생성자", "2026-03")
            val foundVote = voteService.findById(vote.id)

            // Assert
            assertEquals(1, foundVote.voteItems.size)
            assertEquals("기존맛집", foundVote.voteItems[0].restaurant.name)
        }
    }

    @Nested
    @DisplayName("투표하기 통합")
    inner class CastBallotIntegration {

        @Test
        @DisplayName("재투표 시 이전 투표가 대체된다")
        fun replacePreviousBallot_whenReVoting() {
            // Arrange
            authService.loginOrRegister("생성자", "1234")
            authService.loginOrRegister("투표자", "1234")
            restaurantService.register("A식당", "한식", null, "생성자", "2026-03")
            restaurantService.register("B식당", "일식", null, "생성자", "2026-03")

            val vote = voteService.createVote(
                title = "3월 회식",
                yearMonth = "2026-03",
                maxSelections = 2,
                deadline = LocalDateTime.of(2026, 3, 31, 18, 0),
                creatorName = "생성자",
            )

            val itemA = vote.voteItems.first { it.restaurant.name == "A식당" }
            val itemB = vote.voteItems.first { it.restaurant.name == "B식당" }

            val dates = listOf(LocalDate.of(2026, 3, 25))

            // 1차 투표: A식당
            voteService.castBallot(vote.id, listOf(itemA.id), dates, "투표자")

            // Act: 재투표 — B식당으로 변경
            voteService.castBallot(vote.id, listOf(itemB.id), dates, "투표자")

            // Assert
            val myBallotItemIds = voteService.getMyBallotItemIds(vote.id, "투표자")
            assertEquals(1, myBallotItemIds.size)
            assertEquals(itemB.id, myBallotItemIds[0])
        }

        @Test
        @DisplayName("마감된 투표에 투표하면 예외를 던진다")
        fun throwException_whenVoteExpired() {
            // Arrange
            authService.loginOrRegister("생성자", "1234")
            authService.loginOrRegister("투표자", "1234")
            restaurantService.register("맛집", "한식", null, "생성자", "2026-03")

            val vote = voteService.createVote(
                title = "마감될 투표",
                yearMonth = "2026-03",
                maxSelections = 1,
                deadline = LocalDateTime.now().plusSeconds(1),
                creatorName = "생성자",
            )

            // deadline을 과거로 직접 변경
            vote.deadline = LocalDateTime.now().minusHours(1)
            entityManager.flush()

            val itemId = vote.voteItems[0].id

            // Act & Assert
            val exception = assertThrows<CoreException> {
                voteService.castBallot(vote.id, listOf(itemId), listOf(LocalDate.of(2026, 3, 25)), "투표자")
            }
            assertEquals(ErrorType.VOTE_EXPIRED, exception.errorType)
        }
    }

    @Nested
    @DisplayName("날짜 투표 통합")
    inner class DateBallotIntegration {

        @Test
        @DisplayName("날짜 투표를 생성하고 조회한다")
        fun createAndRetrieveDateBallot() {
            // Arrange
            authService.loginOrRegister("생성자", "1234")
            authService.loginOrRegister("투표자", "1234")
            restaurantService.register("맛집", "한식", null, "생성자", "2026-03")

            val vote = voteService.createVote(
                title = "3월 회식",
                yearMonth = "2026-03",
                maxSelections = 1,
                deadline = LocalDateTime.of(2026, 3, 31, 18, 0),
                creatorName = "생성자",
            )

            val selectedDates = listOf(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27),
            )

            // Act
            val itemId = vote.voteItems[0].id
            voteService.castBallot(vote.id, listOf(itemId), selectedDates, "투표자")

            // Assert
            val myDates = voteService.getMyDateBallots(vote.id, "투표자")
            assertEquals(2, myDates.size)
            assertTrue(myDates.contains(LocalDate.of(2026, 3, 25)))
            assertTrue(myDates.contains(LocalDate.of(2026, 3, 27)))

            val summary = voteService.getDateBallotSummary(vote.id)
            assertEquals(1L, summary[LocalDate.of(2026, 3, 25)])
            assertEquals(1L, summary[LocalDate.of(2026, 3, 27)])
        }
    }
}
