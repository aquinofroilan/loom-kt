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
    private val forecastRepository: com.aquinofroilan.tessera.domain.finance.repository.ForecastRepository,
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

        val forecast =
            request.forecastId?.let {
                forecastRepository.findById(it).orElseThrow { ResourceNotFoundException("Forecast not found: ${request.forecastId}") }
            }
        if (forecast != null && forecast.organizationId != organizationId) {
            throw ResourceNotFoundException("Forecast not found: ${request.forecastId}")
        }

        val allAccounts = accountRepository.findByOrganizationId(organizationId).associateBy { it.id }
        val allCostCenters = costCenterRepository.findByOrganizationId(organizationId).associateBy { it.id }

        val periodById = fiscalYear.periods.associateBy { it.id }

        val reportLines = mutableListOf<BudgetVsActualLineResponse>()

        val budgetLinesByPeriod = budget.lines.groupBy { it.fiscalPeriodId }
        val forecastLinesByPeriod = forecast?.lines?.groupBy { it.fiscalPeriodId } ?: emptyMap()

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
            val forecastLinesForPeriod = forecastLinesByPeriod[period.id] ?: emptyList()
            val forecastMap = forecastLinesForPeriod.associateBy { Pair(it.accountId, it.costCenterId) }

            // Track which actuals and forecasts we've matched
            val matchedKeys = mutableSetOf<Pair<UUID?, UUID?>>()

            // 1. Process budget lines and find actuals/encumbrances/forecasts
            for (line in linesForPeriod) {
                val key = Pair(line.accountId, line.costCenterId)
                val actual = actualsMap[key]
                val netActual =
                    actual?.let {
                        it.actualDebits - it.actualCredits
                    } ?: BigDecimal.ZERO
                val netEncumbered =
                    actual?.let {
                        it.encumberedDebits - it.encumberedCredits
                    } ?: BigDecimal.ZERO

                val forecastLine = forecastMap[key]
                val forecastAmount = forecastLine?.amount

                matchedKeys.add(key)

                val account = line.accountId?.let { allAccounts[it] }
                val costCenter = line.costCenterId?.let { allCostCenters[it] }

                val remainingAmount = line.amount - netActual - netEncumbered
                val variancePercentage =
                    if (line.amount.compareTo(BigDecimal.ZERO) != 0) {
                        ((netActual + netEncumbered - line.amount) / line.amount) * BigDecimal("100.00")
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
                        encumberedAmount = netEncumbered,
                        actualAmount = netActual,
                        forecastAmount = forecastAmount,
                        remainingAmount = remainingAmount,
                        variancePercentage = variancePercentage?.setScale(2, RoundingMode.HALF_UP),
                    ),
                )
            }

            // 2. Process actuals or forecasts that have NO budget line
            val allOtherKeys = actualsMap.keys + forecastMap.keys - matchedKeys

            for (key in allOtherKeys) {
                val actual = actualsMap[key]
                val netActual = actual?.let { it.actualDebits - it.actualCredits } ?: BigDecimal.ZERO
                val netEncumbered = actual?.let { it.encumberedDebits - it.encumberedCredits } ?: BigDecimal.ZERO

                val forecastLine = forecastMap[key]
                val forecastAmount = forecastLine?.amount

                if (netActual.compareTo(BigDecimal.ZERO) == 0 &&
                    netEncumbered.compareTo(BigDecimal.ZERO) == 0 &&
                    (forecastAmount == null || forecastAmount.compareTo(BigDecimal.ZERO) == 0)
                ) {
                    continue
                }

                val (accountId, costCenterId) = key

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
                        encumberedAmount = netEncumbered,
                        actualAmount = netActual,
                        forecastAmount = forecastAmount,
                        remainingAmount = BigDecimal.ZERO - netActual - netEncumbered,
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
            forecastId = forecast?.id,
            forecastName = forecast?.name,
            reportDate =
                java.time.LocalDate
                    .now()
                    .toString(),
            lines = reportLines,
        )
    }
}
