package com.aquinofroilan.tessera.domain.finance.dto

import com.aquinofroilan.tessera.domain.finance.model.ExpenseClaimStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ExpenseClaimLineRequest(
    @field:NotNull(message = "Expense date is required")
    val expenseDate: LocalDate?,
    @field:NotBlank(message = "Category is required")
    @field:Size(max = 100)
    val category: String?,
    val categoryId: UUID? = null,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:NotBlank(message = "Original currency is required")
    @field:Size(min = 3, max = 3)
    val originalCurrency: String?,
    @field:NotNull(message = "Original amount is required")
    @field:Positive(message = "Amount must be positive")
    val originalAmount: BigDecimal?,
    val exchangeRate: BigDecimal = BigDecimal.ONE,
    val projectId: UUID? = null,
    @field:Size(max = 1000)
    val receiptUrl: String? = null,
)

data class CreateExpenseClaimRequest(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: UUID?,
    @field:NotNull(message = "Claim date is required")
    val claimDate: LocalDate?,
    @field:NotBlank(message = "Purpose is required")
    @field:Size(max = 1000)
    val purpose: String?,
    @field:NotBlank(message = "Reimbursement currency is required")
    @field:Size(min = 3, max = 3)
    val reimbursementCurrency: String?,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<ExpenseClaimLineRequest>?,
)

data class ExpenseClaimLineResponse(
    val id: UUID,
    val lineNumber: Int,
    val expenseDate: String,
    val category: String,
    val categoryId: UUID?,
    val description: String?,
    val originalCurrency: String,
    val originalAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val reimbursementAmount: BigDecimal,
    val projectId: UUID?,
    val receiptUrl: String?,
)

data class ExpenseClaimResponse(
    val id: UUID,
    val organizationId: UUID,
    val employeeId: UUID,
    val claimDate: String,
    val purpose: String,
    val status: ExpenseClaimStatus,
    val reimbursementCurrency: String,
    val totalReimbursementAmount: BigDecimal,
    val workflowInstanceId: UUID?,
    val journalEntryId: UUID?,
    val paymentJournalEntryId: UUID?,
    val createdBy: UUID,
    val createdAt: String,
    val updatedAt: String?,
    val lines: List<ExpenseClaimLineResponse>,
)
