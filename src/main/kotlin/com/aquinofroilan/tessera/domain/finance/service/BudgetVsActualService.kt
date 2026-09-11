package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.BudgetVsActualLineResponse
import com.aquinofroilan.tessera.domain.finance.dto.BudgetVsActualReportRequest
import com.aquinofroilan.tessera.domain.finance.dto.BudgetVsActualReportResponse
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.BudgetRepository
import com.aquinofroilan.tessera.domain.finance.repository.CostCenterRepository
import com.aquinofroilan.tessera.domain.finance.repository.JournalEntryRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class BudgetVsActualService(
    private val budgetRepository: BudgetRepository,
    private val fiscalYearService: FiscalYearService,
    private val journalEntryRepository: JournalEntryRepository,
    private val accountRepository: AccountRepository,
    private val costCenterRepository: CostCenterRepository,
) {
    @Transactional(readOnly = true)
    fun generateReport(
        organizationId: UUID,
        request: BudgetVsActualReportRequest,
    ): BudgetVsActualReportResponse {
        val budget =
            budgetRepository
                .findById(request.budgetId)
                .orElseThrow { ResourceNotFoundException("Budget not found: ${request.budgetId}") }

        if (budget.organizationId != organizationId) {
            throw ResourceNotFoundException("Budget not found: ${request.budgetId}")
        }

        val fiscalYear = fiscalYearService.getFiscalYear(request.fiscalYearId, organizationId)

        val allAccounts = accountRepository.findByOrganizationId(organizationId).associateBy { it.id }
        val allCostCenters = costCenterRepository.findByOrganizationId(organizationId).associateBy { it.id }

        val periodById = fiscalYear.periods.associateBy { it.id }

        val reportLines = mutableListOf<BudgetVsActualLineResponse>()

        val budgetLinesByPeriod = budget.lines.groupBy { it.fiscalPeriodId }

        for (period in fiscalYear.periods) {
            val actuals =
                journalEntryRepository.aggregateBudgetActuals(
                    organizationId = organizationId,
                    startDate = period.startDate,
                    endDate = period.endDate,
                )

            // Map actuals by (accountId, costCenterId)
            val actualsMap = actuals.associateBy { Pair(it.accountId, it.costCenterId) }

            val linesForPeriod = budgetLinesByPeriod[period.id] ?: emptyList()

            // Track which actuals we've matched to a budget line
            val matchedActualKeys = mutableSetOf<Pair<UUID?, UUID?>>()

            // 1. Process budget lines and find actuals
            for (line in linesForPeriod) {
                val key = Pair(line.accountId, line.costCenterId)
                val actual = actualsMap[key]
                val netActual =
                    actual?.let {
                        it.totalDebits - it.totalCredits // Depending on account type, usually expense is Debit
                    } ?: BigDecimal.ZERO

                matchedActualKeys.add(key)

                val account = line.accountId?.let { allAccounts[it] }
                val costCenter = line.costCenterId?.let { allCostCenters[it] }

                val remainingAmount = line.amount - netActual
                val variancePercentage =
                    if (line.amount.compareTo(BigDecimal.ZERO) != 0) {
                        ((netActual - line.amount) / line.amount) * BigDecimal("100.00")
                    } else {
                        null
                    }

                reportLines.add(
                    BudgetVsActualLineResponse(
                        accountId = line.accountId,
                        accountCode = account?.code,
                        accountName = account?.name,
                        costCenterId = line.costCenterId,
                        costCenterCode = costCenter?.code,
                        costCenterName = costCenter?.name,
                        fiscalPeriodId = period.id,
                        fiscalPeriodName = period.name,
                        budgetedAmount = line.amount,
                        actualAmount = netActual,
                        remainingAmount = remainingAmount,
                        variancePercentage = variancePercentage?.setScale(2, RoundingMode.HALF_UP),
                    ),
                )
            }

            // 2. Process actuals that have NO budget line
            for ((key, actual) in actualsMap) {
                if (key in matchedActualKeys) continue

                val (accountId, costCenterId) = key
                val netActual = actual.totalDebits - actual.totalCredits
                if (netActual.compareTo(BigDecimal.ZERO) == 0) continue

                val account = accountId?.let { allAccounts[it] }
                val costCenter = costCenterId?.let { allCostCenters[it] }

                val variancePercentage = null // Can't divide by zero budget

                reportLines.add(
                    BudgetVsActualLineResponse(
                        accountId = accountId,
                        accountCode = account?.code,
                        accountName = account?.name,
                        costCenterId = costCenterId,
                        costCenterCode = costCenter?.code,
                        costCenterName = costCenter?.name,
                        fiscalPeriodId = period.id,
                        fiscalPeriodName = period.name,
                        budgetedAmount = BigDecimal.ZERO,
                        actualAmount = netActual,
                        remainingAmount = BigDecimal.ZERO - netActual,
                        variancePercentage = variancePercentage,
                    ),
                )
            }
        }

        return BudgetVsActualReportResponse(
            organizationId = organizationId,
            fiscalYearId = fiscalYear.id,
            budgetId = budget.id,
            budgetName = budget.name,
            reportDate = LocalDate.now().toString(),
            lines = reportLines,
        )
    }
}
