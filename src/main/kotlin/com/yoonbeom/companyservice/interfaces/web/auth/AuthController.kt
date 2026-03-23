package com.yoonbeom.companyservice.interfaces.web.auth

import com.yoonbeom.companyservice.application.auth.AuthService
import com.yoonbeom.companyservice.interfaces.web.auth.dto.LoginRequest
import com.yoonbeom.companyservice.support.auth.AuthInterceptor.Companion.SESSION_MEMBER_ID
import com.yoonbeom.companyservice.support.auth.AuthInterceptor.Companion.SESSION_MEMBER_NAME
import com.yoonbeom.companyservice.support.error.CoreException
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping

@Controller
class AuthController(
    private val authService: AuthService,
) {
    @GetMapping("/login")
    fun loginPage(): String = "auth/login"

    @PostMapping("/login")
    fun login(
        @Valid request: LoginRequest,
        bindingResult: BindingResult,
        session: HttpSession,
        model: Model,
    ): String {
        if (bindingResult.hasErrors()) {
            val errorMessage = bindingResult.fieldErrors.first().defaultMessage
            model.addAttribute("errorMessage", errorMessage)
            model.addAttribute("name", request.name)
            return "auth/login"
        }

        try {
            val member = authService.loginOrRegister(request.name.trim(), request.password)
            session.setAttribute(SESSION_MEMBER_ID, member.id)
            session.setAttribute(SESSION_MEMBER_NAME, member.name)
            return "redirect:/"
        } catch (e: CoreException) {
            model.addAttribute("errorMessage", e.message)
            model.addAttribute("name", request.name)
            return "auth/login"
        } catch (e: IllegalArgumentException) {
            model.addAttribute("errorMessage", e.message)
            model.addAttribute("name", request.name)
            return "auth/login"
        }
    }

    @PostMapping("/logout")
    fun logout(session: HttpSession): String {
        session.invalidate()
        return "redirect:/login"
    }
}
