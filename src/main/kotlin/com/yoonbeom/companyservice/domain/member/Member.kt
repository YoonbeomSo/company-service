package com.yoonbeom.companyservice.domain.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "member")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(nullable = false, unique = true, length = 50)
    var name: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        private const val MAX_NAME_LENGTH = 50

        fun create(name: String, hashedPassword: String): Member {
            require(name.isNotBlank()) { "이름은 비어있을 수 없습니다" }
            require(name.length <= MAX_NAME_LENGTH) { "이름은 ${MAX_NAME_LENGTH}자를 초과할 수 없습니다" }

            return Member(
                name = name,
                password = hashedPassword,
            )
        }
    }
}
