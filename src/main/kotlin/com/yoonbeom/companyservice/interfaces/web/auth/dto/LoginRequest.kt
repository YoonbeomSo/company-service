package com.yoonbeom.companyservice.interfaces.web.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class LoginRequest(
    @field:NotBlank(message = "이름을 입력해주세요")
    @field:Pattern(regexp = "^[가-힣]{2,4}$", message = "이름은 한글 2~4자리여야 합니다")
    val name: String = "",

    @field:NotBlank(message = "비밀번호를 입력해주세요")
    @field:Pattern(regexp = "^[0-9]{4}$", message = "비밀번호는 숫자 4자리여야 합니다")
    val password: String = "",
)
