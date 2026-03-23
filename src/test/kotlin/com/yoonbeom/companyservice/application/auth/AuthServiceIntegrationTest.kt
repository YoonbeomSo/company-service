package com.yoonbeom.companyservice.application.auth

import com.yoonbeom.companyservice.domain.member.MemberRepository
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Nested
    @DisplayName("로그인/회원가입 통합")
    inner class LoginOrRegisterIntegration {

        @Test
        @DisplayName("신규 이름이면 회원을 생성하고 DB에 저장한다")
        fun createAndSaveMember_whenNewName() {
            // Arrange
            val name = "테스트유저"
            val rawPassword = "1234"

            // Act
            val member = authService.loginOrRegister(name, rawPassword)

            // Assert
            assertNotNull(member.id)
            assertEquals(name, member.name)
            val savedMember = memberRepository.findByName(name)
            assertNotNull(savedMember)
            assertEquals(name, savedMember.name)
        }

        @Test
        @DisplayName("기존 회원이면 비밀번호 검증 후 반환한다")
        fun returnMember_whenExistingMemberWithCorrectPassword() {
            // Arrange
            val name = "기존유저"
            val rawPassword = "5678"
            authService.loginOrRegister(name, rawPassword)

            // Act
            val member = authService.loginOrRegister(name, rawPassword)

            // Assert
            assertEquals(name, member.name)
        }

        @Test
        @DisplayName("기존 회원이고 비밀번호가 틀리면 예외를 던진다")
        fun throwException_whenWrongPassword() {
            // Arrange
            val name = "기존유저2"
            val rawPassword = "1234"
            authService.loginOrRegister(name, rawPassword)

            // Act & Assert
            val exception = assertThrows<CoreException> {
                authService.loginOrRegister(name, "9999")
            }
            assertEquals(ErrorType.PASSWORD_MISMATCH, exception.errorType)
        }
    }
}
