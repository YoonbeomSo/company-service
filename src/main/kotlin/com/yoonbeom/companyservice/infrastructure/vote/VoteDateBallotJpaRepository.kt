package com.yoonbeom.companyservice.infrastructure.vote

import com.yoonbeom.companyservice.domain.vote.VoteDateBallot
import com.yoonbeom.companyservice.domain.vote.VoteDateBallotRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VoteDateBallotJpaRepository : JpaRepository<VoteDateBallot, Long>, VoteDateBallotRepository {

    @Modifying
    @Query("DELETE FROM VoteDateBallot vdb WHERE vdb.vote.id = :voteId AND vdb.voter.id = :voterId")
    override fun deleteAllByVoteIdAndVoterId(voteId: Long, voterId: Long)

    override fun findAllByVoteIdAndVoterId(voteId: Long, voterId: Long): List<VoteDateBallot>
    override fun findAllByVoteId(voteId: Long): List<VoteDateBallot>
}
