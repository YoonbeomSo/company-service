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
        private val NAME_PATTERN = Regex("^[가-힣]{2,4}$")

        fun create(name: String, hashedPassword: String): Member {
            require(name.matches(NAME_PATTERN)) { "이름은 한글 2~4자리여야 합니다" }

            return Member(
                name = name,
                password = hashedPassword,
            )
        }
    }
}
