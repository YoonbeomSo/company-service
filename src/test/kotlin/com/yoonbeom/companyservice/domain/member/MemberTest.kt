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
        @DisplayName("한글 3자 이름으로 회원을 생성한다")
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
        @DisplayName("한글 2자 이름으로 회원을 생성한다")
        fun createMember_whenTwoCharName() {
            // Arrange & Act
            val member = Member.create("홍길", "\$2a\$10\$hash")

            // Assert
            assertEquals("홍길", member.name)
        }

        @Test
        @DisplayName("한글 4자 이름으로 회원을 생성한다 (경계값)")
        fun createMember_whenFourCharName() {
            // Arrange & Act
            val member = Member.create("홍길동이", "\$2a\$10\$hash")

            // Assert
            assertEquals("홍길동이", member.name)
        }

        @Test
        @DisplayName("이름이 빈 문자열이면 예외를 던진다")
        fun throwException_whenNameIsBlank() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create("", "\$2a\$10\$hash")
            }
        }

        @Test
        @DisplayName("이름이 5자 이상이면 예외를 던진다")
        fun throwException_whenNameExceedsMaxLength() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create("홍길동이삼", "\$2a\$10\$hash")
            }
        }

        @Test
        @DisplayName("이름이 1자면 예외를 던진다")
        fun throwException_whenNameIsSingleChar() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create("홍", "\$2a\$10\$hash")
            }
        }

        @Test
        @DisplayName("영어 이름이면 예외를 던진다")
        fun throwException_whenNameIsEnglish() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create("abc", "\$2a\$10\$hash")
            }
        }

        @Test
        @DisplayName("이름에 숫자가 포함되면 예외를 던진다")
        fun throwException_whenNameContainsNumber() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create("홍길1", "\$2a\$10\$hash")
            }
        }

        @Test
        @DisplayName("이름이 공백문자만 포함하면 예외를 던진다")
        fun throwException_whenNameIsOnlyWhitespace() {
            // Arrange & Act & Assert
            assertThrows<IllegalArgumentException> {
                Member.create("   ", "\$2a\$10\$hash")
            }
        }
    }
}
