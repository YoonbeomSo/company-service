package com.yoonbeom.companyservice.support.error

import org.springframework.http.HttpStatus

enum class ErrorType(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "400", "잘못된 요청입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증이 필요합니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "404", "리소스를 찾을 수 없습니다"),
    CONFLICT(HttpStatus.CONFLICT, "409", "이미 존재하는 리소스입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류입니다"),

    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_001", "비밀번호가 일치하지 않습니다"),
    VOTE_EXPIRED(HttpStatus.BAD_REQUEST, "VOTE_001", "투표가 마감되었습니다"),
    VOTE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "VOTE_002", "최대 선택 수를 초과했습니다"),
}
