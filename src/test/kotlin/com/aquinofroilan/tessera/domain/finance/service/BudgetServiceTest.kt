package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.BudgetLineRequest
import com.aquinofroilan.tessera.domain.finance.dto.CreateBudgetRequest
import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.AccountType
import com.aquinofroilan.tessera.domain.finance.model.Budget
import com.aquinofroilan.tessera.domain.finance.model.BudgetStatus
import com.aquinofroilan.tessera.domain.finance.model.FiscalPeriod
import com.aquinofroilan.tessera.domain.finance.model.FiscalYear
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.BudgetRepository
import com.aquinofroilan.tessera.domain.finance.repository.CostCenterRepository
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

class BudgetServiceTest {
    private val budgetRepository: BudgetRepository = mock()
    private val fiscalYearService: FiscalYearService = mock()
    private val accountRepository: AccountRepository = mock()
    private val costCenterRepository: CostCenterRepository = mock()

    private val service =
        BudgetService(
            budgetRepository,
            fiscalYearService,
            accountRepository,
            costCenterRepository,
        )

    @Test
    fun `should create budget`() {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val fyId = UUID.randomUUID()
        val periodId = UUID.randomUUID()
        val accId = UUID.randomUUID()

        val period =
            FiscalPeriod(
                id = periodId,
                periodNumber = 1,
                name = "Jan",
                startDate = LocalDate.now(),
                endDate = LocalDate.now(),
            )
        val fy =
            FiscalYear(
                id = fyId,
                organizationId = orgId,
                name = "FY 2026",
                startDate = LocalDate.now(),
                endDate = LocalDate.now(),
                periods = listOf(period),
            )
        whenever(fiscalYearService.getFiscalYear(fyId, orgId)).thenReturn(fy)

        val acc =
            Account(
                id = accId,
                organizationId = orgId,
                name = "Revenue",
                code = "REV",
                type = AccountType.REVENUE,
            )
        whenever(accountRepository.findById(accId)).thenReturn(Optional.of(acc))
        whenever(budgetRepository.findByOrganizationIdAndFiscalYearId(orgId, fyId)).thenReturn(emptyList())
        whenever(budgetRepository.save(any<Budget>())).thenAnswer { it.arguments[0] as Budget }

        val request =
            CreateBudgetRequest(
                fiscalYearId = fyId,
                name = "Annual Budget",
                lines =
                    listOf(
                        BudgetLineRequest(
                            accountId = accId,
                            costCenterId = null,
                            fiscalPeriodId = periodId,
                            amount = BigDecimal("10000.00"),
                        ),
                    ),
            )

        val res = service.createBudget(orgId, userId, request)
        assertNotNull(res)
        assertEquals("Annual Budget", res.name)
        assertEquals(1, res.version)
        assertEquals(BudgetStatus.DRAFT, res.status)
        assertEquals(1, res.lines.size)
        assertEquals(BigDecimal("10000.00"), res.lines[0].amount)
    }
}
