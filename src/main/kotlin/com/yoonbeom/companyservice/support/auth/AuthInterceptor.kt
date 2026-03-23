package com.yoonbeom.companyservice.support.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val memberId = request.session.getAttribute(SESSION_MEMBER_ID)
        if (memberId == null) {
            response.sendRedirect("/login")
            return false
        }
        return true
    }

    companion object {
        const val SESSION_MEMBER_ID = "memberId"
        const val SESSION_MEMBER_NAME = "memberName"
    }
}
