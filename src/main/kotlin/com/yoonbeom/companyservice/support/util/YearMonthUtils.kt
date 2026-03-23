package com.yoonbeom.companyservice.support.util

import java.time.YearMonth
import java.time.format.DateTimeFormatter

object YearMonthUtils {

    val YEAR_MONTH_PATTERN: Regex = Regex("^\\d{4}-\\d{2}$")

    private val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun formatDisplay(yearMonth: String): String {
        val ym = YearMonth.parse(yearMonth)
        return "${ym.year}년 ${ym.monthValue}월"
    }

    fun prev(yearMonth: String): String =
        YearMonth.parse(yearMonth).minusMonths(1).format(YEAR_MONTH_FORMATTER)

    fun next(yearMonth: String): String =
        YearMonth.parse(yearMonth).plusMonths(1).format(YEAR_MONTH_FORMATTER)

    fun currentOrDefault(yearMonth: String?): String =
        yearMonth ?: YearMonth.now().format(YEAR_MONTH_FORMATTER)
}
