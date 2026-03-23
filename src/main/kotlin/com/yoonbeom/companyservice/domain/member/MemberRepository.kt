package com.yoonbeom.companyservice.domain.member

interface MemberRepository {
    fun findByName(name: String): Member?
    fun save(member: Member): Member
}
