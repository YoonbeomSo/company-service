package com.yoonbeom.companyservice.infrastructure.member

import com.yoonbeom.companyservice.domain.member.Member
import com.yoonbeom.companyservice.domain.member.MemberRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberJpaRepository : JpaRepository<Member, Long>, MemberRepository {
    override fun findByName(name: String): Member?
}
