package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.BudgetVsActualReportRequest
import com.aquinofroilan.tessera.domain.finance.model.Budget
import com.aquinofroilan.tessera.domain.finance.model.BudgetLine
import com.aquinofroilan.tessera.domain.finance.model.BudgetStatus
import com.aquinofroilan.tessera.domain.finance.model.FiscalPeriod
import com.aquinofroilan.tessera.domain.finance.model.FiscalYear
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.BudgetActualAggregation
import com.aquinofroilan.tessera.domain.finance.repository.BudgetRepository
import com.aquinofroilan.tessera.domain.finance.repository.CostCenterRepository
import com.aquinofroilan.tessera.domain.finance.repository.JournalEntryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class BudgetVsActualServiceTest {
    private val budgetRepository: BudgetRepository = mock()
    private val fiscalYearService: FiscalYearService = mock()
    private val journalEntryRepository: JournalEntryRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val costCenterRepository: CostCenterRepository = mock()

    private val service =
        BudgetVsActualService(
            budgetRepository,
            fiscalYearService,
            journalEntryRepository,
            accountRepository,
            costCenterRepository,
        )

    @Test
    fun `should generate budget vs actual report`() {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val fyId = UUID.randomUUID()
        val budgetId = UUID.randomUUID()
        val periodId = UUID.randomUUID()
        val accId = UUID.randomUUID()

        val period =
            FiscalPeriod(
                id = periodId,
                periodNumber = 1,
                name = "Jan",
                startDate = LocalDate.now().withDayOfMonth(1),
                endDate = LocalDate.now().withDayOfMonth(28),
            )
        val fy =
            FiscalYear(
                id = fyId,
                organizationId = orgId,
                name = "FY 2026",
                startDate = period.startDate,
                endDate = period.endDate,
                periods = listOf(period),
            )
        whenever(fiscalYearService.getFiscalYear(fyId, orgId)).thenReturn(fy)

        val budget =
            Budget(
                id = budgetId,
                organizationId = orgId,
                fiscalYearId = fyId,
                name = "Annual Budget",
                version = 1,
                status = BudgetStatus.ACTIVE,
                createdBy = userId,
            )
        budget.lines.add(
            BudgetLine(
                accountId = accId,
                costCenterId = null,
                fiscalPeriodId = periodId,
                amount = BigDecimal("10000.00"),
            ),
        )

        whenever(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget))
        whenever(accountRepository.findByOrganizationId(orgId)).thenReturn(emptyList())
        whenever(costCenterRepository.findByOrganizationId(orgId)).thenReturn(emptyList())

        val aggregations =
            listOf(
                BudgetActualAggregation(
                    accountId = accId,
                    costCenterId = null,
                    totalDebits = BigDecimal("6000.00"),
                    totalCredits = BigDecimal("1000.00"), // net actual is 5000.00
                ),
            )
        whenever(journalEntryRepository.aggregateBudgetActuals(any(), any(), any())).thenReturn(aggregations)

        val request =
            BudgetVsActualReportRequest(
                fiscalYearId = fyId,
                budgetId = budgetId,
            )

        val res = service.generateReport(orgId, request)
        assertNotNull(res)
        assertEquals(1, res.lines.size)

        val line = res.lines[0]
        assertEquals(accId, line.accountId)
        assertEquals(BigDecimal("10000.00"), line.budgetedAmount)
        assertEquals(BigDecimal("5000.00"), line.actualAmount)
        assertEquals(BigDecimal("5000.00"), line.remainingAmount)
        assertEquals(BigDecimal("-50.00"), line.variancePercentage)
    }
}
