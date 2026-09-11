package com.aquinofroilan.tessera.domain.finance.dto

import com.aquinofroilan.tessera.domain.finance.model.CashAdvanceStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class IssueCashAdvanceRequest(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: UUID?,
    @field:NotNull(message = "Date is required")
    val date: LocalDate?,
    @field:NotBlank(message = "Purpose is required")
    @field:Size(max = 1000)
    val purpose: String?,
    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal?,
    @field:NotBlank(message = "Currency is required")
    @field:Size(min = 3, max = 3)
    val currency: String?,
    @field:NotNull(message = "Cash account ID is required")
    val cashAccountId: UUID?,
    @field:NotNull(message = "Advance receivable account ID is required")
    val advanceAccountId: UUID?,
)

data class CashAdvanceResponse(
    val id: UUID,
    val organizationId: UUID,
    val employeeId: UUID,
    val date: String,
    val purpose: String,
    val amount: BigDecimal,
    val currency: String,
    val status: CashAdvanceStatus,
    val journalEntryId: UUID?,
    val createdBy: UUID,
    val createdAt: String,
    val updatedAt: String?,
)
