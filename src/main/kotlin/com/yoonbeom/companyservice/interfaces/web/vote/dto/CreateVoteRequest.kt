package com.yoonbeom.companyservice.interfaces.web.vote.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class CreateVoteRequest(
    @field:NotBlank(message = "투표 제목을 입력해주세요")
    val title: String = "",

    @field:NotBlank(message = "대상 월을 선택해주세요")
    val yearMonth: String = "",

    @field:Min(value = 1, message = "최소 1개 이상 선택 가능해야 합니다")
    val maxSelections: Int = 1,

    @field:NotBlank(message = "마감시간을 입력해주세요")
    val deadline: String = "",

    val restaurantIds: List<Long> = emptyList(),

    val includeDateVote: Boolean = true,
)
