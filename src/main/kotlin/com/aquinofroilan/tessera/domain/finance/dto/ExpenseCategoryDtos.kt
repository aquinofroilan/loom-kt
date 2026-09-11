package com.aquinofroilan.tessera.domain.finance.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class CreateExpenseCategoryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100)
    val name: String?,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:NotNull(message = "Expense account ID is required")
    val expenseAccountId: UUID?,
    @field:Positive(message = "Policy limit must be positive")
    val policyLimit: BigDecimal? = null,
    @field:Size(min = 3, max = 3)
    val limitCurrency: String? = null,
    val isActive: Boolean = true,
)

data class UpdateExpenseCategoryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100)
    val name: String?,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:NotNull(message = "Expense account ID is required")
    val expenseAccountId: UUID?,
    @field:Positive(message = "Policy limit must be positive")
    val policyLimit: BigDecimal? = null,
    @field:Size(min = 3, max = 3)
    val limitCurrency: String? = null,
    val isActive: Boolean?,
)

data class ExpenseCategoryResponse(
    val id: UUID,
    val organizationId: UUID,
    val name: String,
    val description: String?,
    val expenseAccountId: UUID,
    val policyLimit: BigDecimal?,
    val limitCurrency: String?,
    val isActive: Boolean,
    val createdBy: UUID,
    val createdAt: String,
    val updatedAt: String?,
)
