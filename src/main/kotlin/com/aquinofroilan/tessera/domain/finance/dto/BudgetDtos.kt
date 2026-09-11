package com.aquinofroilan.tessera.domain.finance.dto

import com.aquinofroilan.tessera.domain.finance.model.BudgetStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class BudgetLineRequest(
    val accountId: UUID?,
    val costCenterId: UUID?,
    @field:NotNull(message = "Fiscal period ID is required")
    val fiscalPeriodId: UUID?,
    @field:NotNull(message = "Amount is required")
    @field:PositiveOrZero(message = "Amount must be zero or positive")
    val amount: BigDecimal?,
)

data class CreateBudgetRequest(
    @field:NotNull(message = "Fiscal year ID is required")
    val fiscalYearId: UUID?,
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255)
    val name: String?,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<BudgetLineRequest>?,
)

data class BudgetLineResponse(
    val id: UUID,
    val accountId: UUID?,
    val costCenterId: UUID?,
    val fiscalPeriodId: UUID,
    val amount: BigDecimal,
)

data class BudgetResponse(
    val id: UUID,
    val organizationId: UUID,
    val fiscalYearId: UUID,
    val name: String,
    val version: Int,
    val status: BudgetStatus,
    val lines: List<BudgetLineResponse>,
    val createdBy: UUID,
    val createdAt: String,
    val updatedAt: String?,
)
