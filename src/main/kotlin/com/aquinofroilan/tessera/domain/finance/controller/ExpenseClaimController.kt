package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.domain.finance.dto.CreateExpenseClaimRequest
import com.aquinofroilan.tessera.domain.finance.dto.ExpenseClaimResponse
import com.aquinofroilan.tessera.domain.finance.service.ExpenseClaimService
import com.aquinofroilan.tessera.security.AuthenticationContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/expenses")
class ExpenseClaimController(
    private val expenseClaimService: ExpenseClaimService,
    private val authenticationContext: AuthenticationContext,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'finance:write') or hasPermission(null, 'expenses:write')")
    fun createExpenseClaim(
        @Valid @RequestBody request: CreateExpenseClaimRequest,
    ): ExpenseClaimResponse =
        expenseClaimService.createClaim(
            organizationId = authenticationContext.organizationId()!!,
            userId = authenticationContext.userId()!!,
            request = request,
        )

    @GetMapping
    @PreAuthorize("hasPermission(null, 'finance:read') or hasPermission(null, 'expenses:read')")
    fun listExpenseClaims(
        @RequestParam(required = false) employeeId: UUID?,
    ): List<ExpenseClaimResponse> =
        expenseClaimService.listClaims(
            organizationId = authenticationContext.organizationId()!!,
            employeeId = employeeId,
        )

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'finance:read') or hasPermission(null, 'expenses:read')")
    fun getExpenseClaim(
        @PathVariable id: UUID,
    ): ExpenseClaimResponse =
        expenseClaimService.getClaimResponse(id, authenticationContext.organizationId()!!)

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasPermission(null, 'finance:write') or hasPermission(null, 'expenses:write')")
    fun submitExpenseClaim(
        @PathVariable id: UUID,
    ): ExpenseClaimResponse =
        expenseClaimService.submitClaim(
            organizationId = authenticationContext.organizationId()!!,
            claimId = id,
            userId = authenticationContext.userId()!!,
        )

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasPermission(null, 'finance:write')")
    fun approveExpenseClaim(
        @PathVariable id: UUID,
        @RequestParam expenseAccountId: UUID,
        @RequestParam payableAccountId: UUID,
    ): ExpenseClaimResponse =
        expenseClaimService.approveClaim(
            organizationId = authenticationContext.organizationId()!!,
            claimId = id,
            userId = authenticationContext.userId()!!,
            expenseAccountId = expenseAccountId,
            payableAccountId = payableAccountId,
        )

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasPermission(null, 'finance:write')")
    fun rejectExpenseClaim(
        @PathVariable id: UUID,
    ): ExpenseClaimResponse =
        expenseClaimService.rejectClaim(
            organizationId = authenticationContext.organizationId()!!,
            claimId = id,
            userId = authenticationContext.userId()!!,
        )
}
