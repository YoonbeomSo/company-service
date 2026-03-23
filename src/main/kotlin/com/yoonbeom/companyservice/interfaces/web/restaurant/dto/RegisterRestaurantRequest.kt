package com.yoonbeom.companyservice.interfaces.web.restaurant.dto

import jakarta.validation.constraints.NotBlank

data class RegisterRestaurantRequest(
    @field:NotBlank(message = "가게이름을 입력해주세요")
    val name: String = "",

    @field:NotBlank(message = "음식종류를 입력해주세요")
    val foodType: String = "",

    val link: String? = null,
)
