package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.BudgetVsActualReportRequest
import com.aquinofroilan.tessera.domain.finance.dto.BudgetVsActualReportResponse
import com.aquinofroilan.tessera.domain.finance.service.BudgetVsActualService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/reports")
@Loggable(level = LogLevel.INFO)
class BudgetVsActualController(
    private val budgetVsActualService: BudgetVsActualService,
) {
    @GetMapping("/budget-vs-actual/{budgetId}")
    @PreAuthorize("hasAuthority('finance:read')")
    fun generateReport(
        @RequestAttribute("organizationId") orgId: UUID,
        @PathVariable budgetId: UUID,
        @RequestParam("fiscalYearId") fiscalYearId: UUID,
    ): ResponseEntity<BudgetVsActualReportResponse> {
        val request =
            BudgetVsActualReportRequest(
                fiscalYearId = fiscalYearId,
                budgetId = budgetId,
            )
        val response = budgetVsActualService.generateReport(orgId, request)
        return ResponseEntity.ok(response)
    }
}
