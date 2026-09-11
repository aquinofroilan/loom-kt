package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.domain.finance.dto.CreateExpenseCategoryRequest
import com.aquinofroilan.tessera.domain.finance.dto.ExpenseCategoryResponse
import com.aquinofroilan.tessera.domain.finance.dto.UpdateExpenseCategoryRequest
import com.aquinofroilan.tessera.domain.finance.service.ExpenseCategoryService
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
@RequestMapping("/api/v1/finance/expense-categories")
class ExpenseCategoryController(
    private val expenseCategoryService: ExpenseCategoryService,
    private val authenticationContext: AuthenticationContext,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'finance:write')")
    fun createExpenseCategory(
        @Valid @RequestBody request: CreateExpenseCategoryRequest,
    ): ExpenseCategoryResponse =
        expenseCategoryService.createCategory(
            organizationId = authenticationContext.organizationId()!!,
            userId = authenticationContext.userId()!!,
            request = request,
        )

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'finance:write')")
    fun updateExpenseCategory(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateExpenseCategoryRequest,
    ): ExpenseCategoryResponse =
        expenseCategoryService.updateCategory(
            organizationId = authenticationContext.organizationId()!!,
            categoryId = id,
            request = request,
        )

    @GetMapping
    @PreAuthorize("hasPermission(null, 'finance:read') or hasPermission(null, 'expenses:read')")
    fun listExpenseCategories(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): List<ExpenseCategoryResponse> =
        expenseCategoryService.listCategories(
            organizationId = authenticationContext.organizationId()!!,
            activeOnly = activeOnly,
        )

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'finance:read') or hasPermission(null, 'expenses:read')")
    fun getExpenseCategory(
        @PathVariable id: UUID,
    ): ExpenseCategoryResponse = expenseCategoryService.getCategoryResponse(id, authenticationContext.organizationId()!!)
}
