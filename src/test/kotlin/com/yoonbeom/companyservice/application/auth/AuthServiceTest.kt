package com.yoonbeom.companyservice.application.auth

import com.yoonbeom.companyservice.domain.member.Member
import com.yoonbeom.companyservice.domain.member.MemberRepository
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder

    @InjectMocks
    lateinit var authService: AuthService

    @Nested
    @DisplayName("로그인/회원가입")
    inner class LoginOrRegister {

        @Test
        @DisplayName("존재하지 않는 이름이면 회원을 생성하고 반환한다")
        fun createMember_whenNameNotExists() {
            // Arrange
            val name = "홍길동"
            val rawPassword = "1234"
            val hashedPassword = "\$2a\$10\$hashedPassword"
            val savedMember = Member.create(name, hashedPassword)

            whenever(memberRepository.findByName(name)).thenReturn(null)
            whenever(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword)
            whenever(memberRepository.save(any())).thenReturn(savedMember)

            // Act
            val result = authService.loginOrRegister(name, rawPassword)

            // Assert
            assertEquals(name, result.name)
            verify(memberRepository).save(any())
        }

        @Test
        @DisplayName("존재하는 이름이고 비밀번호가 일치하면 회원을 반환한다")
        fun returnMember_whenPasswordMatches() {
            // Arrange
            val name = "홍길동"
            val rawPassword = "1234"
            val hashedPassword = "\$2a\$10\$hashedPassword"
            val existingMember = Member.create(name, hashedPassword)

            whenever(memberRepository.findByName(name)).thenReturn(existingMember)
            whenever(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true)

            // Act
            val result = authService.loginOrRegister(name, rawPassword)

            // Assert
            assertEquals(name, result.name)
        }

        @Test
        @DisplayName("존재하는 이름이고 비밀번호가 불일치하면 예외를 던진다")
        fun throwException_whenPasswordDoesNotMatch() {
            // Arrange
            val name = "홍길동"
            val rawPassword = "1234"
            val hashedPassword = "\$2a\$10\$hashedPassword"
            val existingMember = Member.create(name, hashedPassword)

            whenever(memberRepository.findByName(name)).thenReturn(existingMember)
            whenever(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(false)

            // Act & Assert
            val exception = assertThrows<CoreException> {
                authService.loginOrRegister(name, rawPassword)
            }
            assertEquals(ErrorType.PASSWORD_MISMATCH, exception.errorType)
        }
    }
}
