package com.yoonbeom.companyservice.domain.member

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MemberTest {

    @Nested
    @DisplayName("회원 생성")
    inner class Create {

        @Test
        @DisplayName("이름과 해시된 비밀번호로 회원을 생성한다")
        fun createMember_whenValidInput() {
            // Arrange
            val name = "홍길동"
            val hashedPassword = "\$2a\$10\$dummyHashedPassword"

            // Act
            val member = Member.create(name, hashedPassword)

            // Assert
            assertEquals(name, member.name)
            assertEquals(hashedPassword, member.password)
            assertNotNull(member.createdAt)
        }

        @Test
        @DisplayName("이름이 빈 문자열이면 예외를 던진다")
        fun throwException_whenNameIsBlank() {
            // Arrange
            val name = ""
            val hashedPassword = "\$2a\$10\$dummyHashedPassword"

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create(name, hashedPassword)
            }
        }

        @Test
        @DisplayName("이름이 50자를 초과하면 예외를 던진다")
        fun throwException_whenNameExceedsMaxLength() {
            // Arrange
            val name = "a".repeat(51)
            val hashedPassword = "\$2a\$10\$dummyHashedPassword"

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create(name, hashedPassword)
            }
        }

        @Test
        @DisplayName("이름이 정확히 50자면 정상 생성된다")
        fun createMember_whenNameIsExactly50Characters() {
            // Arrange
            val name = "a".repeat(50)
            val hashedPassword = "\$2a\$10\$dummyHashedPassword"

            // Act
            val member = Member.create(name, hashedPassword)

            // Assert
            assertEquals(name, member.name)
            assertEquals(50, member.name.length)
        }

        @Test
        @DisplayName("이름이 공백문자만 포함하면 예외를 던진다")
        fun throwException_whenNameIsOnlyWhitespace() {
            // Arrange
            val name = "   "
            val hashedPassword = "\$2a\$10\$dummyHashedPassword"

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create(name, hashedPassword)
            }
        }
    }

}
