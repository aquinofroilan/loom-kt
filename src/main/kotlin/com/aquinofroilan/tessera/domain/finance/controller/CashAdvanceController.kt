package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.domain.finance.dto.CashAdvanceResponse
import com.aquinofroilan.tessera.domain.finance.dto.IssueCashAdvanceRequest
import com.aquinofroilan.tessera.domain.finance.service.CashAdvanceService
import com.aquinofroilan.tessera.security.AuthenticationContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/cash-advances")
class CashAdvanceController(
    private val cashAdvanceService: CashAdvanceService,
    private val authenticationContext: AuthenticationContext,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'finance:write')")
    fun issueCashAdvance(
        @Valid @RequestBody request: IssueCashAdvanceRequest,
    ): CashAdvanceResponse =
        cashAdvanceService.issueAdvance(
            organizationId = authenticationContext.organizationId()!!,
            userId = authenticationContext.userId()!!,
            request = request,
        )

    @GetMapping("/outstanding")
    @PreAuthorize("hasPermission(null, 'finance:read') or hasPermission(null, 'expenses:read')")
    fun getOutstandingAdvances(
        @RequestParam employeeId: UUID,
    ): List<CashAdvanceResponse> =
        cashAdvanceService.getOutstandingAdvances(
            organizationId = authenticationContext.organizationId()!!,
            employeeId = employeeId,
        )
}
