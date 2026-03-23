package com.yoonbeom.companyservice.infrastructure.vote

import com.yoonbeom.companyservice.domain.vote.VoteItem
import com.yoonbeom.companyservice.domain.vote.VoteItemRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteItemJpaRepository : JpaRepository<VoteItem, Long>, VoteItemRepository {
    override fun findAllByVoteId(voteId: Long): List<VoteItem>
}
