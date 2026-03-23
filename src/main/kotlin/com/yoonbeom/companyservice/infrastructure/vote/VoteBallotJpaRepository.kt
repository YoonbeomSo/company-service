package com.yoonbeom.companyservice.infrastructure.vote

import com.yoonbeom.companyservice.domain.vote.VoteBallot
import com.yoonbeom.companyservice.domain.vote.VoteBallotRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VoteBallotJpaRepository : JpaRepository<VoteBallot, Long>, VoteBallotRepository {

    @Modifying
    @Query("DELETE FROM VoteBallot vb WHERE vb.voteItem.vote.id = :voteId AND vb.voter.id = :voterId")
    override fun deleteAllByVoteItemVoteIdAndVoterId(voteId: Long, voterId: Long)

    override fun findAllByVoteItemVoteIdAndVoterId(voteId: Long, voterId: Long): List<VoteBallot>
}
