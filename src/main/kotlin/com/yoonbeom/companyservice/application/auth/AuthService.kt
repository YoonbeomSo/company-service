package com.yoonbeom.companyservice.application.auth

import com.yoonbeom.companyservice.domain.member.Member
import com.yoonbeom.companyservice.domain.member.MemberRepository
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun loginOrRegister(name: String, rawPassword: String): Member {
        val existingMember = memberRepository.findByName(name)

        if (existingMember == null) {
            val hashedPassword = passwordEncoder.encode(rawPassword)
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "비밀번호 인코딩 실패")
            val newMember = Member.create(name, hashedPassword)
            return memberRepository.save(newMember)
        }

        if (!passwordEncoder.matches(rawPassword, existingMember.password)) {
            throw CoreException(ErrorType.PASSWORD_MISMATCH)
        }

        return existingMember
    }
}
