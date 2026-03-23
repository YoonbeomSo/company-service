package com.yoonbeom.companyservice.infrastructure.vote

import com.yoonbeom.companyservice.domain.vote.Vote
import com.yoonbeom.companyservice.domain.vote.VoteRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VoteJpaRepository : JpaRepository<Vote, Long>, VoteRepository {

    @Query("SELECT v FROM Vote v WHERE v.yearMonth = :yearMonth ORDER BY v.createdAt DESC")
    override fun findAllByYearMonth(yearMonth: String): List<Vote>

    @Query("SELECT DISTINCT v FROM Vote v LEFT JOIN FETCH v.voteItems vi LEFT JOIN FETCH vi.restaurant WHERE v.id = :id")
    override fun findVoteById(id: Long): Vote?
}
