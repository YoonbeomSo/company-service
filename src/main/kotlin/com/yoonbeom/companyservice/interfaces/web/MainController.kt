package com.yoonbeom.companyservice.interfaces.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MainController {

    @GetMapping("/")
    fun index(): String = "redirect:/votes"
}
