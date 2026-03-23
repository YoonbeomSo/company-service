package com.yoonbeom.companyservice.domain.vote

interface VoteBallotRepository {
    fun <S : VoteBallot> saveAll(entities: Iterable<S>): List<S>
    fun deleteAllByVoteItemVoteIdAndVoterId(voteId: Long, voterId: Long)
    fun findAllByVoteItemVoteIdAndVoterId(voteId: Long, voterId: Long): List<VoteBallot>
}
