package com.yoonbeom.companyservice.domain.vote

import com.yoonbeom.companyservice.domain.restaurant.Restaurant
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.BatchSize

@Entity
@Table(
    name = "vote_item",
    uniqueConstraints = [UniqueConstraint(columnNames = ["vote_id", "restaurant_id"])],
)
class VoteItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    var vote: Vote,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    var restaurant: Restaurant,

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "voteItem", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var ballots: MutableList<VoteBallot> = mutableListOf(),
)
