package com.yoonbeom.companyservice.domain.vote

import com.yoonbeom.companyservice.domain.member.Member
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import com.yoonbeom.companyservice.support.util.YearMonthUtils
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "vote")
class Vote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(name = "target_month", nullable = false, length = 7)
    var yearMonth: String,

    @Column(name = "max_selections", nullable = false)
    var maxSelections: Int,

    @Column(nullable = false)
    var deadline: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    var createdBy: Member,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "vote", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var voteItems: MutableList<VoteItem> = mutableListOf(),

    @OneToMany(mappedBy = "vote", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var dateBallots: MutableList<VoteDateBallot> = mutableListOf(),
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(deadline)

    fun validateCanVote(selectedCount: Int) {
        if (isExpired()) throw CoreException(ErrorType.VOTE_EXPIRED)
        if (selectedCount > maxSelections) throw CoreException(ErrorType.VOTE_LIMIT_EXCEEDED)
    }

    fun getWinners(): List<VoteItem> {
        if (voteItems.isEmpty()) return emptyList()

        val maxVotes = voteItems.maxOf { it.ballots.size }
        if (maxVotes == 0) return emptyList()

        return voteItems.filter { it.ballots.size == maxVotes }
    }

    companion object {
        fun create(
            title: String,
            yearMonth: String,
            maxSelections: Int,
            deadline: LocalDateTime,
            createdBy: Member,
        ): Vote {
            require(title.isNotBlank()) { "투표 제목은 비어있을 수 없습니다" }
            require(maxSelections >= 1) { "최대 선택 수는 1 이상이어야 합니다" }
            require(yearMonth.matches(YearMonthUtils.YEAR_MONTH_PATTERN)) { "yearMonth 형식은 YYYY-MM이어야 합니다" }
            require(deadline.isAfter(LocalDateTime.now())) { "마감시간은 현재 시간 이후여야 합니다" }

            return Vote(
                title = title,
                yearMonth = yearMonth,
                maxSelections = maxSelections,
                deadline = deadline,
                createdBy = createdBy,
            )
        }
    }
}
