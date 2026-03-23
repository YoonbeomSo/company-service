package com.yoonbeom.companyservice.support.error

import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CoreException::class)
    fun handleCoreException(
        ex: CoreException,
        model: Model,
    ): String {
        model.addAttribute("errorMessage", ex.message)
        return "error"
    }
}
