package com.yoonbeom.companyservice.domain.vote

interface VoteDateBallotRepository {
    fun <S : VoteDateBallot> saveAll(entities: Iterable<S>): List<S>
    fun deleteAllByVoteIdAndVoterId(voteId: Long, voterId: Long)
    fun findAllByVoteIdAndVoterId(voteId: Long, voterId: Long): List<VoteDateBallot>
    fun findAllByVoteId(voteId: Long): List<VoteDateBallot>
}
