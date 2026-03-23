package com.yoonbeom.companyservice.application.vote

import com.yoonbeom.companyservice.domain.member.Member
import com.yoonbeom.companyservice.domain.member.MemberRepository
import com.yoonbeom.companyservice.domain.restaurant.Restaurant
import com.yoonbeom.companyservice.domain.restaurant.RestaurantRepository
import com.yoonbeom.companyservice.domain.vote.Vote
import com.yoonbeom.companyservice.domain.vote.VoteBallotRepository
import com.yoonbeom.companyservice.domain.vote.VoteDateBallotRepository
import com.yoonbeom.companyservice.domain.vote.VoteItem
import com.yoonbeom.companyservice.domain.vote.VoteItemRepository
import com.yoonbeom.companyservice.domain.vote.VoteRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class VoteServiceTest {

    @Mock lateinit var voteRepository: VoteRepository
    @Mock lateinit var voteItemRepository: VoteItemRepository
    @Mock lateinit var voteBallotRepository: VoteBallotRepository
    @Mock lateinit var voteDateBallotRepository: VoteDateBallotRepository
    @Mock lateinit var restaurantRepository: RestaurantRepository
    @Mock lateinit var memberRepository: MemberRepository
    @Mock lateinit var entityManager: jakarta.persistence.EntityManager

    @InjectMocks
    lateinit var voteService: VoteService

    private fun createMember(id: Long = 1L, name: String = "유저"): Member {
        val m = Member.create(name, "\$2a\$10\$hash")
        m.id = id
        return m
    }

    private fun createRestaurant(id: Long, name: String = "맛집"): Restaurant {
        val r = Restaurant.create(name, "한식", null, createMember(), "2026-03")
        r.id = id
        return r
    }

    @Nested
    @DisplayName("투표 생성")
    inner class CreateVote {

        @Test
        @DisplayName("후보지 목록으로 투표를 생성한다")
        fun createVote_whenValidInput() {
            // Arrange
            val creator = createMember(1L, "생성자")
            val restaurants = listOf(createRestaurant(1L, "A식당"), createRestaurant(2L, "B식당"))
            val deadline = LocalDateTime.of(2026, 3, 25, 18, 0)

            whenever(memberRepository.findByName("생성자")).thenReturn(creator)
            whenever(restaurantRepository.findAllByYearMonth("2026-03")).thenReturn(restaurants)
            whenever(voteRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(voteItemRepository.saveAll(any<List<VoteItem>>())).thenAnswer { it.arguments[0] as List<*> }

            // Act
            val vote = voteService.createVote(
                title = "3월 회식",
                yearMonth = "2026-03",
                maxSelections = 1,
                deadline = deadline,
                creatorName = "생성자",
            )

            // Assert
            assertEquals("3월 회식", vote.title)
            verify(voteRepository).save(any())
        }
    }

    @Nested
    @DisplayName("투표하기")
    inner class CastBallot {

        @Test
        @DisplayName("존재하지 않는 투표에 투표하면 NOT_FOUND 예외를 던진다")
        fun throwException_whenVoteNotFound() {
            // Arrange
            whenever(voteRepository.findVoteById(999L)).thenReturn(null)

            // Act & Assert
            val exception = assertThrows<CoreException> {
                voteService.castBallot(999L, listOf(1L), listOf(java.time.LocalDate.of(2026, 3, 25)), "투표자")
            }
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
        }

        @Test
        @DisplayName("정상적으로 투표하면 기존 투표 삭제 후 새 투표가 저장된다")
        fun saveNewBallot_whenValidVote() {
            // Arrange
            val voter = createMember(2L, "투표자")
            val vote = Vote.create("회식", "2026-03", 2, LocalDateTime.now().plusHours(1), createMember(1L, "생성자"))
            vote.id = 1L

            val item1 = VoteItem(id = 1L, vote = vote, restaurant = createRestaurant(1L, "A식당"))
            vote.voteItems = mutableListOf(item1)

            whenever(voteRepository.findVoteById(1L)).thenReturn(vote)
            whenever(memberRepository.findByName("투표자")).thenReturn(voter)
            whenever(voteBallotRepository.saveAll(any<List<com.yoonbeom.companyservice.domain.vote.VoteBallot>>()))
                .thenAnswer { it.arguments[0] as List<*> }
            whenever(voteDateBallotRepository.saveAll(any<List<com.yoonbeom.companyservice.domain.vote.VoteDateBallot>>()))
                .thenAnswer { it.arguments[0] as List<*> }

            // Act
            voteService.castBallot(1L, listOf(1L), listOf(java.time.LocalDate.of(2026, 3, 25)), "투표자")

            // Assert
            verify(voteBallotRepository).deleteAllByVoteItemVoteIdAndVoterId(1L, 2L)
            verify(voteBallotRepository).saveAll(any<List<com.yoonbeom.companyservice.domain.vote.VoteBallot>>())
            verify(voteDateBallotRepository).deleteAllByVoteIdAndVoterId(1L, 2L)
        }

        @Test
        @DisplayName("마감된 투표에 투표하면 예외를 던진다")
        fun throwException_whenVoteExpired() {
            // Arrange
            val member = createMember(1L, "투표자")
            val vote = Vote(
                title = "회식", yearMonth = "2026-03", maxSelections = 1,
                deadline = LocalDateTime.now().minusHours(1), createdBy = member,
            )
            vote.id = 1L

            whenever(voteRepository.findVoteById(1L)).thenReturn(vote)

            // Act & Assert
            val exception = assertThrows<CoreException> {
                voteService.castBallot(1L, listOf(1L), listOf(java.time.LocalDate.of(2026, 3, 25)), "투표자")
            }
            assertEquals(ErrorType.VOTE_EXPIRED, exception.errorType)
        }

        @Test
        @DisplayName("최대 선택수를 초과하면 예외를 던진다")
        fun throwException_whenExceedMaxSelections() {
            // Arrange
            val member = createMember(1L, "투표자")
            val vote = Vote.create("회식", "2026-03", 1, LocalDateTime.now().plusHours(1), member)
            vote.id = 1L

            val item1 = VoteItem(id = 1L, vote = vote, restaurant = createRestaurant(1L))
            val item2 = VoteItem(id = 2L, vote = vote, restaurant = createRestaurant(2L))
            vote.voteItems = mutableListOf(item1, item2)

            whenever(voteRepository.findVoteById(1L)).thenReturn(vote)

            // Act & Assert
            val exception = assertThrows<CoreException> {
                voteService.castBallot(1L, listOf(1L, 2L), listOf(java.time.LocalDate.of(2026, 3, 25)), "투표자")
            }
            assertEquals(ErrorType.VOTE_LIMIT_EXCEEDED, exception.errorType)
        }
    }
}
