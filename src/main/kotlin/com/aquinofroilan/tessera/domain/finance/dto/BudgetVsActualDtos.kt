package com.aquinofroilan.tessera.domain.finance.dto

import java.math.BigDecimal
import java.util.UUID

data class BudgetVsActualReportRequest(
    val fiscalYearId: UUID,
    val budgetId: UUID,
)

data class BudgetVsActualLineResponse(
    val accountId: UUID?,
    val accountCode: String?,
    val accountName: String?,
    val costCenterId: UUID?,
    val costCenterCode: String?,
    val costCenterName: String?,
    val fiscalPeriodId: UUID,
    val fiscalPeriodName: String,
    val budgetedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val variancePercentage: BigDecimal?,
)

data class BudgetVsActualReportResponse(
    val organizationId: UUID,
    val fiscalYearId: UUID,
    val budgetId: UUID,
    val budgetName: String,
    val reportDate: String,
    val lines: List<BudgetVsActualLineResponse>,
)
