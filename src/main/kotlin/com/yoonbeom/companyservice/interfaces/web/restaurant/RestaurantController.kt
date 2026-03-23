package com.yoonbeom.companyservice.interfaces.web.restaurant

import com.yoonbeom.companyservice.application.restaurant.RestaurantService
import com.yoonbeom.companyservice.interfaces.web.restaurant.dto.RegisterRestaurantRequest
import com.yoonbeom.companyservice.support.auth.AuthInterceptor.Companion.SESSION_MEMBER_NAME
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import com.yoonbeom.companyservice.support.util.YearMonthUtils
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class RestaurantController(
    private val restaurantService: RestaurantService,
) {
    @GetMapping("/restaurants")
    fun list(
        @RequestParam(required = false) yearMonth: String?,
        model: Model,
    ): String {
        val currentYearMonth = YearMonthUtils.currentOrDefault(yearMonth)
        val restaurants = restaurantService.findByYearMonth(currentYearMonth)

        model.addAttribute("activeTab", "restaurants")
        model.addAttribute("yearMonth", currentYearMonth)
        model.addAttribute("restaurants", restaurants)
        model.addAttribute("yearMonthDisplay", YearMonthUtils.formatDisplay(currentYearMonth))
        model.addAttribute("prevYearMonth", YearMonthUtils.prev(currentYearMonth))
        model.addAttribute("nextYearMonth", YearMonthUtils.next(currentYearMonth))

        return "restaurant/list"
    }

    @PostMapping("/restaurants")
    fun register(
        @Valid request: RegisterRestaurantRequest,
        @RequestParam yearMonth: String,
        session: HttpSession,
        model: Model,
    ): String {
        val memberName = session.getAttribute(SESSION_MEMBER_NAME) as? String
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        restaurantService.register(
            name = request.name.trim(),
            foodType = request.foodType.trim(),
            link = request.link?.trim(),
            registrantName = memberName,
            yearMonth = yearMonth,
        )

        val restaurants = restaurantService.findByYearMonth(yearMonth)
        model.addAttribute("restaurants", restaurants)

        return "restaurant/fragments/restaurant-list"
    }
}
