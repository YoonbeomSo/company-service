package com.yoonbeom.companyservice.domain.vote

import com.yoonbeom.companyservice.domain.member.Member
import com.yoonbeom.companyservice.domain.restaurant.Restaurant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoteTest {

    private fun createMember(name: String = "홍길동"): Member {
        val member = Member.create(name, "\$2a\$10\$hash")
        member.id = 1L
        return member
    }

    private fun createRestaurant(name: String = "맛집"): Restaurant {
        val restaurant = Restaurant.create(name, "한식", null, createMember(), "2026-03")
        restaurant.id = 1L
        return restaurant
    }

    private fun createExpiredVote(): Vote = Vote(
        title = "마감된 투표",
        yearMonth = "2026-03",
        maxSelections = 1,
        deadline = LocalDateTime.now().minusHours(1),
        createdBy = createMember(),
    )

    @Nested
    @DisplayName("투표 생성")
    inner class Create {

        @Test
        @DisplayName("유효한 정보로 투표를 생성한다")
        fun createVote_whenValidInput() {
            // Arrange
            val creator = createMember()

            // Act
            val vote = Vote.create(
                title = "3월 1차 회식",
                yearMonth = "2026-03",
                maxSelections = 2,
                deadline = LocalDateTime.of(2026, 3, 25, 18, 0),
                createdBy = creator,
            )

            // Assert
            assertEquals("3월 1차 회식", vote.title)
            assertEquals("2026-03", vote.yearMonth)
            assertEquals(2, vote.maxSelections)
            assertNotNull(vote.createdAt)
        }

        @Test
        @DisplayName("제목이 빈 문자열이면 예외를 던진다")
        fun throwException_whenTitleIsBlank() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Vote.create("", "2026-03", 1, LocalDateTime.now().plusDays(1), createMember())
            }
        }

        @Test
        @DisplayName("maxSelections가 0 이하면 예외를 던진다")
        fun throwException_whenMaxSelectionsInvalid() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Vote.create("회식", "2026-03", 0, LocalDateTime.now().plusDays(1), createMember())
            }
        }

        @Test
        @DisplayName("yearMonth 형식이 잘못되면 예외를 던진다")
        fun throwException_whenYearMonthFormatInvalid() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Vote.create("회식", "2026/03", 1, LocalDateTime.now().plusDays(1), createMember())
            }
        }

        @Test
        @DisplayName("yearMonth에 날짜까지 포함하면 예외를 던진다")
        fun throwException_whenYearMonthContainsDay() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Vote.create("회식", "2026-03-15", 1, LocalDateTime.now().plusDays(1), createMember())
            }
        }

        @Test
        @DisplayName("마감시간이 과거이면 예외를 던진다")
        fun throwException_whenDeadlineInPast() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Vote.create("회식", "2026-03", 1, LocalDateTime.now().minusHours(1), createMember())
            }
        }
    }

    @Nested
    @DisplayName("투표 마감 확인")
    inner class IsExpired {

        @Test
        @DisplayName("마감시간이 지나면 true를 반환한다")
        fun returnTrue_whenDeadlinePassed() {
            // Arrange
            val vote = createExpiredVote()

            // Act & Assert
            assertTrue(vote.isExpired())
        }

        @Test
        @DisplayName("마감시간 전이면 false를 반환한다")
        fun returnFalse_whenBeforeDeadline() {
            // Arrange
            val vote = Vote.create(
                "회식", "2026-03", 1,
                LocalDateTime.now().plusHours(1),
                createMember(),
            )

            // Act & Assert
            assertFalse(vote.isExpired())
        }
    }

    @Nested
    @DisplayName("투표 결과 (1등)")
    inner class GetWinners {

        @Test
        @DisplayName("가장 많은 득표를 받은 항목을 반환한다")
        fun returnTopVotedItem() {
            // Arrange
            val vote = createExpiredVote()

            val item1 = VoteItem(id = 1L, vote = vote, restaurant = createRestaurant("A식당"))
            val item2 = VoteItem(id = 2L, vote = vote, restaurant = createRestaurant("B식당"))
            vote.voteItems = mutableListOf(item1, item2)

            val voter1 = createMember("김철수").also { it.id = 2L }
            val voter2 = createMember("이영희").also { it.id = 3L }
            val voter3 = createMember("박민수").also { it.id = 4L }

            item1.ballots = mutableListOf(
                VoteBallot(id = 1L, voteItem = item1, voter = voter1),
                VoteBallot(id = 2L, voteItem = item1, voter = voter2),
            )
            item2.ballots = mutableListOf(
                VoteBallot(id = 3L, voteItem = item2, voter = voter3),
            )

            // Act
            val winners = vote.getWinners()

            // Assert
            assertEquals(1, winners.size)
            assertEquals("A식당", winners[0].restaurant.name)
        }

        @Test
        @DisplayName("아무도 투표하지 않은 경우 빈 리스트를 반환한다")
        fun returnEmptyList_whenNoBallots() {
            // Arrange
            val vote = createExpiredVote()

            val item1 = VoteItem(id = 1L, vote = vote, restaurant = createRestaurant("A식당"))
            val item2 = VoteItem(id = 2L, vote = vote, restaurant = createRestaurant("B식당"))
            vote.voteItems = mutableListOf(item1, item2)

            // Act
            val winners = vote.getWinners()

            // Assert
            assertTrue(winners.isEmpty())
        }

        @Test
        @DisplayName("voteItems가 비어있으면 빈 리스트를 반환한다")
        fun returnEmptyList_whenNoVoteItems() {
            // Arrange
            val vote = createExpiredVote()

            // Act
            val winners = vote.getWinners()

            // Assert
            assertTrue(winners.isEmpty())
        }

        @Test
        @DisplayName("득표수가 같으면 공동 1등을 반환한다")
        fun returnMultipleWinners_whenTied() {
            // Arrange
            val vote = createExpiredVote()

            val item1 = VoteItem(id = 1L, vote = vote, restaurant = createRestaurant("A식당"))
            val item2 = VoteItem(id = 2L, vote = vote, restaurant = createRestaurant("B식당"))
            vote.voteItems = mutableListOf(item1, item2)

            val voter1 = createMember("김철수").also { it.id = 2L }
            val voter2 = createMember("이영희").also { it.id = 3L }

            item1.ballots = mutableListOf(
                VoteBallot(id = 1L, voteItem = item1, voter = voter1),
            )
            item2.ballots = mutableListOf(
                VoteBallot(id = 2L, voteItem = item2, voter = voter2),
            )

            // Act
            val winners = vote.getWinners()

            // Assert
            assertEquals(2, winners.size)
        }
    }
}
