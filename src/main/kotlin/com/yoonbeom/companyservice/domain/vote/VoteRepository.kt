package com.yoonbeom.companyservice.domain.vote

interface VoteRepository {
    fun save(vote: Vote): Vote
    fun findVoteById(id: Long): Vote?
    fun findAllByYearMonth(yearMonth: String): List<Vote>
}
