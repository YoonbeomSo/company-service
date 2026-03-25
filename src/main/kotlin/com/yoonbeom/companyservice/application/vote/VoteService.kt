package com.yoonbeom.companyservice.application.vote

import com.yoonbeom.companyservice.domain.member.MemberRepository
import com.yoonbeom.companyservice.domain.restaurant.RestaurantRepository
import com.yoonbeom.companyservice.domain.vote.Vote
import com.yoonbeom.companyservice.domain.vote.VoteBallot
import com.yoonbeom.companyservice.domain.vote.VoteBallotRepository
import com.yoonbeom.companyservice.domain.vote.VoteDateBallot
import com.yoonbeom.companyservice.domain.vote.VoteDateBallotRepository
import com.yoonbeom.companyservice.domain.vote.VoteItem
import com.yoonbeom.companyservice.domain.vote.VoteItemRepository
import com.yoonbeom.companyservice.domain.vote.VoteRepository
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class VoteService(
    private val voteRepository: VoteRepository,
    private val voteItemRepository: VoteItemRepository,
    private val voteBallotRepository: VoteBallotRepository,
    private val voteDateBallotRepository: VoteDateBallotRepository,
    private val restaurantRepository: RestaurantRepository,
    private val memberRepository: MemberRepository,
    private val entityManager: EntityManager,
) {
    @Transactional
    fun createVote(
        title: String,
        yearMonth: String,
        maxSelections: Int,
        deadline: LocalDateTime,
        creatorName: String,
        restaurantIds: List<Long> = emptyList(),
    ): Vote {
        val creator = memberRepository.findByName(creatorName)
            ?: throw CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다")

        val vote = Vote.create(title, yearMonth, maxSelections, deadline, creator)
        val savedVote = voteRepository.save(vote)

        val restaurants = if (restaurantIds.isEmpty()) {
            restaurantRepository.findAllByYearMonth(yearMonth)
        } else {
            restaurantRepository.findAllById(restaurantIds)
        }

        if (restaurants.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "선택된 후보지가 없습니다")
        }

        val voteItems = restaurants.map { VoteItem(vote = savedVote, restaurant = it) }
        voteItemRepository.saveAll(voteItems)

        savedVote.voteItems = voteItems.toMutableList()
        return savedVote
    }

    @Transactional(readOnly = true)
    fun findByYearMonth(yearMonth: String): List<Vote> {
        return voteRepository.findAllByYearMonth(yearMonth)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Vote {
        val vote = voteRepository.findVoteById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "투표를 찾을 수 없습니다")
        vote.voteItems.forEach { it.ballots.size }
        return vote
    }

    @Transactional
    fun closeVote(voteId: Long) {
        val vote = voteRepository.findVoteById(voteId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "투표를 찾을 수 없습니다")
        vote.deadline = LocalDateTime.now()
        voteRepository.save(vote)
    }

    @Transactional
    fun castBallot(
        voteId: Long,
        selectedItemIds: List<Long>,
        selectedDates: List<LocalDate>,
        voterName: String,
    ) {
        if (selectedItemIds.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "음식점을 1개 이상 선택해주세요")
        }
        if (selectedDates.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "날짜를 1개 이상 선택해주세요")
        }

        val vote = voteRepository.findVoteById(voteId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "투표를 찾을 수 없습니다")

        vote.validateCanVote(selectedItemIds.size)

        val voter = memberRepository.findByName(voterName)
            ?: throw CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다")

        // 기존 투표 삭제 후 영속성 컨텍스트 동기화
        voteBallotRepository.deleteAllByVoteItemVoteIdAndVoterId(voteId, voter.id)
        voteDateBallotRepository.deleteAllByVoteIdAndVoterId(voteId, voter.id)
        entityManager.flush()
        entityManager.clear()

        // 재투표 - vote를 다시 조회 (clear 후 영속성 컨텍스트가 비워졌으므로)
        val freshVote = voteRepository.findVoteById(voteId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "투표를 찾을 수 없습니다")

        val validItems = freshVote.voteItems.filter { it.id in selectedItemIds }
        val ballots = validItems.map { VoteBallot(voteItem = it, voter = voter) }
        voteBallotRepository.saveAll(ballots)
        // 날짜 투표 재저장 (freshVote, voter를 다시 조회)
        val freshVoter = memberRepository.findByName(voterName)
            ?: throw CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다")
        val dateBallots = selectedDates.map { VoteDateBallot(vote = freshVote, voter = freshVoter, availableDate = it) }
        voteDateBallotRepository.saveAll(dateBallots)
    }

    @Transactional(readOnly = true)
    fun getMyBallotItemIds(voteId: Long, voterName: String): List<Long> {
        val voter = memberRepository.findByName(voterName) ?: return emptyList()
        return voteBallotRepository.findAllByVoteItemVoteIdAndVoterId(voteId, voter.id)
            .map { it.voteItem.id }
    }

    @Transactional(readOnly = true)
    fun getMyDateBallots(voteId: Long, voterName: String): List<LocalDate> {
        val voter = memberRepository.findByName(voterName) ?: return emptyList()
        return voteDateBallotRepository.findAllByVoteIdAndVoterId(voteId, voter.id)
            .map { it.availableDate }
    }

    @Transactional(readOnly = true)
    fun getDateBallotSummary(voteId: Long): Map<LocalDate, Long> {
        return voteDateBallotRepository.findAllByVoteId(voteId)
            .groupBy { it.availableDate }
            .mapValues { it.value.size.toLong() }
            .toSortedMap()
    }

    @Transactional(readOnly = true)
    fun getDateBallotVoterNames(voteId: Long): Map<LocalDate, List<String>> {
        return voteDateBallotRepository.findAllByVoteId(voteId)
            .groupBy { it.availableDate }
            .mapValues { entry -> entry.value.map { it.voter.name } }
            .toSortedMap()
    }
}
