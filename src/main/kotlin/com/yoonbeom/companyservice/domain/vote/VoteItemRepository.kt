package com.yoonbeom.companyservice.domain.vote

interface VoteItemRepository {
    fun <S : VoteItem> saveAll(entities: Iterable<S>): List<S>
    fun findAllByVoteId(voteId: Long): List<VoteItem>
}
