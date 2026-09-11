package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.IssueCashAdvanceRequest
import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.AccountType
import com.aquinofroilan.tessera.domain.finance.model.CashAdvance
import com.aquinofroilan.tessera.domain.finance.model.CashAdvanceStatus
import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryStatus
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.CashAdvanceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class CashAdvanceServiceTest {
    private val cashAdvanceRepository: CashAdvanceRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val journalEntryService: JournalEntryService = mock()

    private val service =
        CashAdvanceService(
            cashAdvanceRepository,
            accountRepository,
            journalEntryService,
        )

    @Test
    fun `should issue cash advance`() {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val empId = UUID.randomUUID()
        val cashAccId = UUID.randomUUID()
        val advAccId = UUID.randomUUID()

        val cashAcc =
            Account(
                id = cashAccId,
                organizationId = orgId,
                name = "Cash",
                code = "CASH",
                type = AccountType.ASSET,
                isActive = true,
            )
        val advAcc =
            Account(
                id = advAccId,
                organizationId = orgId,
                name = "Advance",
                code = "ADV",
                type = AccountType.ASSET,
                isActive = true,
            )

        whenever(accountRepository.findAllById(any())).thenReturn(listOf(cashAcc, advAcc))

        val advance =
            CashAdvance(
                organizationId = orgId,
                employeeId = empId,
                date = LocalDate.now(),
                purpose = "Travel",
                amount = BigDecimal("500.00"),
                currency = "USD",
                createdBy = userId,
            )
        whenever(cashAdvanceRepository.save(any<CashAdvance>())).thenAnswer { it.arguments[0] as CashAdvance }

        val je =
            JournalEntry(
                organizationId = orgId,
                entryNumber = "JE-1",
                date = LocalDate.now(),
                description = "test",
                status = JournalEntryStatus.POSTED,
                createdBy = userId,
                lines = emptyList(),
            )
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any())).thenReturn(je)

        val req =
            IssueCashAdvanceRequest(
                employeeId = empId,
                date = LocalDate.now(),
                purpose = "Travel",
                amount = BigDecimal("500.00"),
                currency = "USD",
                cashAccountId = cashAccId,
                advanceAccountId = advAccId,
            )

        val res = service.issueAdvance(orgId, userId, req)

        assertNotNull(res)
        assertEquals("Travel", res.purpose)
        assertEquals(CashAdvanceStatus.ISSUED, res.status)
        assertEquals(BigDecimal("500.00"), res.amount)
    }
}
