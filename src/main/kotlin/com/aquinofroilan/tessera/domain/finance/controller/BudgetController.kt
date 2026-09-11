package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.BudgetResponse
import com.aquinofroilan.tessera.domain.finance.dto.CreateBudgetRequest
import com.aquinofroilan.tessera.domain.finance.service.BudgetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/budgets")
@Loggable(level = LogLevel.INFO)
class BudgetController(
    private val budgetService: BudgetService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('finance:write')")
    fun createBudget(
        @RequestAttribute("organizationId") orgId: UUID,
        @RequestAttribute("userId") userId: UUID,
        @Valid @RequestBody request: CreateBudgetRequest,
    ): ResponseEntity<BudgetResponse> {
        val response = budgetService.createBudget(orgId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('finance:write')")
    fun activateBudget(
        @RequestAttribute("organizationId") orgId: UUID,
        @RequestAttribute("userId") userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<BudgetResponse> {
        val response = budgetService.activateBudget(orgId, id, userId)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('finance:write')")
    fun closeBudget(
        @RequestAttribute("organizationId") orgId: UUID,
        @RequestAttribute("userId") userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<BudgetResponse> {
        val response = budgetService.closeBudget(orgId, id, userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:read')")
    fun getBudget(
        @RequestAttribute("organizationId") orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<BudgetResponse> {
        val response = budgetService.getBudgetResponse(id, orgId)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance:read')")
    fun listBudgets(
        @RequestAttribute("organizationId") orgId: UUID,
        @RequestParam("fiscalYearId") fiscalYearId: UUID,
    ): ResponseEntity<List<BudgetResponse>> {
        val response = budgetService.listBudgets(orgId, fiscalYearId)
        return ResponseEntity.ok(response)
    }
}
