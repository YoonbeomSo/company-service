package com.yoonbeom.companyservice.interfaces.web.vote

import com.yoonbeom.companyservice.application.vote.VoteService
import com.yoonbeom.companyservice.interfaces.web.vote.dto.CreateVoteRequest
import com.yoonbeom.companyservice.support.auth.AuthInterceptor.Companion.SESSION_MEMBER_NAME
import com.yoonbeom.companyservice.support.error.CoreException
import com.yoonbeom.companyservice.support.error.ErrorType
import com.yoonbeom.companyservice.support.util.YearMonthUtils
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Controller
class VoteController(
    private val voteService: VoteService,
) {
    companion object {
        private const val ADMIN_NAME = "관리자"
    }
    @GetMapping("/votes")
    fun list(
        @RequestParam(required = false) yearMonth: String?,
        session: HttpSession,
        model: Model,
    ): String {
        val currentYearMonth = YearMonthUtils.currentOrDefault(yearMonth)
        val votes = voteService.findByYearMonth(currentYearMonth)
        val memberName = session.getAttribute(SESSION_MEMBER_NAME) as? String

        model.addAttribute("activeTab", "votes")
        model.addAttribute("isAdmin", memberName == ADMIN_NAME)
        model.addAttribute("yearMonth", currentYearMonth)
        model.addAttribute("votes", votes)
        model.addAttribute("yearMonthDisplay", YearMonthUtils.formatDisplay(currentYearMonth))
        model.addAttribute("prevYearMonth", YearMonthUtils.prev(currentYearMonth))
        model.addAttribute("nextYearMonth", YearMonthUtils.next(currentYearMonth))

        return "vote/list"
    }

    @PostMapping("/votes")
    fun create(
        @Valid request: CreateVoteRequest,
        session: HttpSession,
    ): String {
        val memberName = session.getAttribute(SESSION_MEMBER_NAME) as? String
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        if (memberName != ADMIN_NAME) {
            throw CoreException(ErrorType.UNAUTHORIZED, "관리자만 투표를 생성할 수 있습니다")
        }

        val deadline = LocalDateTime.parse(request.deadline)

        val vote = voteService.createVote(
            title = request.title.trim(),
            yearMonth = request.yearMonth,
            maxSelections = request.maxSelections,
            deadline = deadline,
            creatorName = memberName,
        )

        return "redirect:/votes/${vote.id}"
    }

    @GetMapping("/votes/{id}")
    fun detail(
        @PathVariable id: Long,
        session: HttpSession,
        model: Model,
    ): String {
        val vote = voteService.findById(id)
        val memberName = session.getAttribute(SESSION_MEMBER_NAME) as? String
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        val myBallotItemIds = voteService.getMyBallotItemIds(id, memberName)
        val myDateBallots = voteService.getMyDateBallots(id, memberName)
        val dateSummary = voteService.getDateBallotSummary(id)

        val ym = YearMonth.parse(vote.yearMonth)
        val calendarDates = (1..ym.lengthOfMonth()).map { ym.atDay(it) }
        // 일(0), 월(1), 화(2)...토(6) — Java DayOfWeek: MON=1...SUN=7
        val firstDayOffset = calendarDates[0].dayOfWeek.value % 7

        val maxBallotCount = vote.voteItems.maxOfOrNull { it.ballots.size } ?: 0
        val totalBallotCount = vote.voteItems.sumOf { it.ballots.size }
        val voterNames = vote.voteItems
            .flatMap { item -> item.ballots.map { it.voter.name } }
            .distinct()

        val maxDateCount = dateSummary.values.maxOrNull() ?: 0L

        val itemRankMap = buildRankMap(vote.voteItems.associate { it.id to it.ballots.size.toLong() })
        val dateRankMap = buildRankMap(dateSummary)

        model.addAttribute("activeTab", "votes")
        model.addAttribute("isAdmin", memberName == ADMIN_NAME)
        model.addAttribute("today", LocalDate.now())
        model.addAttribute("firstDayOffset", firstDayOffset)
        model.addAttribute("vote", vote)
        model.addAttribute("myBallotItemIds", myBallotItemIds)
        model.addAttribute("myDateBallots", myDateBallots)
        model.addAttribute("dateSummary", dateSummary)
        model.addAttribute("maxDateCount", maxDateCount)
        model.addAttribute("calendarDates", calendarDates)
        model.addAttribute("expired", vote.isExpired())
        model.addAttribute("maxBallotCount", maxBallotCount)
        model.addAttribute("totalBallotCount", totalBallotCount)
        model.addAttribute("voterNames", voterNames)
        model.addAttribute("itemRankMap", itemRankMap)
        model.addAttribute("dateRankMap", dateRankMap)
        model.addAttribute("dateVoterNames", voteService.getDateBallotVoterNames(id))

        return "vote/detail"
    }

    @PostMapping("/votes/{id}/close")
    fun closeVote(
        @PathVariable id: Long,
        session: HttpSession,
    ): String {
        val memberName = session.getAttribute(SESSION_MEMBER_NAME) as? String
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        if (memberName != ADMIN_NAME) {
            throw CoreException(ErrorType.UNAUTHORIZED, "관리자만 투표를 종료할 수 있습니다")
        }

        voteService.closeVote(id)
        return "redirect:/votes/$id"
    }

    @PostMapping("/votes/{id}/ballot")
    fun castBallot(
        @PathVariable id: Long,
        @RequestParam(required = false) selectedItems: List<Long>?,
        @RequestParam(required = false) selectedDates: List<String>?,
        session: HttpSession,
        model: Model,
    ): String {
        val memberName = session.getAttribute(SESSION_MEMBER_NAME) as? String
            ?: throw CoreException(ErrorType.UNAUTHORIZED)
        val dates = selectedDates?.map { LocalDate.parse(it) } ?: emptyList()

        voteService.castBallot(id, selectedItems ?: emptyList(), dates, memberName)

        return "redirect:/votes/$id"
    }

    @GetMapping("/votes/{id}/result-fragment")
    fun resultFragment(
        @PathVariable id: Long,
        session: HttpSession,
        model: Model,
    ): String {
        val vote = voteService.findById(id)
        val dateSummary = voteService.getDateBallotSummary(id)

        val maxBallotCount = vote.voteItems.maxOfOrNull { it.ballots.size } ?: 0
        val totalBallotCount = vote.voteItems.sumOf { it.ballots.size }
        val voterNames = vote.voteItems
            .flatMap { item -> item.ballots.map { it.voter.name } }
            .distinct()

        val maxDateCount = dateSummary.values.maxOrNull() ?: 0L

        val itemRankMap = buildRankMap(vote.voteItems.associate { it.id to it.ballots.size.toLong() })
        val dateRankMap = buildRankMap(dateSummary)

        model.addAttribute("vote", vote)
        model.addAttribute("dateSummary", dateSummary)
        model.addAttribute("expired", vote.isExpired())
        model.addAttribute("maxBallotCount", maxBallotCount)
        model.addAttribute("totalBallotCount", totalBallotCount)
        model.addAttribute("maxDateCount", maxDateCount)
        model.addAttribute("voterNames", voterNames)
        model.addAttribute("itemRankMap", itemRankMap)
        model.addAttribute("dateRankMap", dateRankMap)
        model.addAttribute("dateVoterNames", voteService.getDateBallotVoterNames(id))

        return "vote/fragments/result"
    }

    // 값 기준 순위 계산 (0 제외, 동점 시 같은 순위)
    private fun <K> buildRankMap(data: Map<K, Long>): Map<K, Int> {
        val sorted = data.filter { it.value > 0 }.values.distinct().sortedDescending()
        return data.mapValues { (_, count) ->
            if (count == 0L) 0 else sorted.indexOf(count) + 1
        }
    }
}
