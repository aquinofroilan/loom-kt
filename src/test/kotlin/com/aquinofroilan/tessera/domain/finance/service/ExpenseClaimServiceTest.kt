package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateExpenseClaimRequest
import com.aquinofroilan.tessera.domain.finance.dto.ExpenseClaimLineRequest
import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.AccountType
import com.aquinofroilan.tessera.domain.finance.model.ExpenseCategory
import com.aquinofroilan.tessera.domain.finance.model.ExpenseClaim
import com.aquinofroilan.tessera.domain.finance.model.ExpenseClaimLine
import com.aquinofroilan.tessera.domain.finance.model.ExpenseClaimStatus
import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntrySource
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryStatus
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.CashAdvanceRepository
import com.aquinofroilan.tessera.domain.finance.repository.ExpenseCategoryRepository
import com.aquinofroilan.tessera.domain.finance.repository.ExpenseClaimRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class ExpenseClaimServiceTest {
    private val expenseClaimRepository: ExpenseClaimRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val journalEntryService: JournalEntryService = mock()
    private val expenseCategoryRepository: ExpenseCategoryRepository = mock()
    private val cashAdvanceRepository: CashAdvanceRepository = mock()

    private lateinit var expenseClaimService: ExpenseClaimService

    private val orgId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val empId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        expenseClaimService =
            ExpenseClaimService(
                expenseClaimRepository,
                accountRepository,
                journalEntryService,
                expenseCategoryRepository,
                cashAdvanceRepository,
            )
    }

    @Test
    fun `createClaim calculates total reimbursement amount correctly`() {
        whenever(expenseClaimRepository.save(any<ExpenseClaim>())).thenAnswer { it.arguments[0] as ExpenseClaim }

        val request =
            CreateExpenseClaimRequest(
                employeeId = empId,
                claimDate = LocalDate.now(),
                purpose = "Business Trip",
                reimbursementCurrency = "USD",
                lines =
                    listOf(
                        ExpenseClaimLineRequest(
                            expenseDate = LocalDate.now(),
                            category = "Meals",
                            description = "Dinner",
                            originalCurrency = "EUR",
                            originalAmount = BigDecimal("100"),
                            exchangeRate = BigDecimal("1.1000"),
                        ),
                        ExpenseClaimLineRequest(
                            expenseDate = LocalDate.now(),
                            category = "Transport",
                            description = "Taxi",
                            originalCurrency = "USD",
                            originalAmount = BigDecimal("50"),
                            exchangeRate = BigDecimal("1.0000"),
                        ),
                    ),
            )

        val response = expenseClaimService.createClaim(orgId, userId, request)

        assertEquals(BigDecimal("160.0000"), response.totalReimbursementAmount)
        assertEquals(ExpenseClaimStatus.DRAFT, response.status)
        assertEquals(2, response.lines.size)
    }

    @Test
    fun `submitClaim changes status to SUBMITTED`() {
        val claimId = UUID.randomUUID()
        val claim =
            ExpenseClaim(
                id = claimId,
                organizationId = orgId,
                employeeId = empId,
                claimDate = LocalDate.now(),
                purpose = "Business Trip",
                reimbursementCurrency = "USD",
                createdBy = userId,
            )
        whenever(expenseClaimRepository.findById(claimId)).thenReturn(Optional.of(claim))
        whenever(expenseClaimRepository.save(any<ExpenseClaim>())).thenAnswer { it.arguments[0] as ExpenseClaim }

        val response = expenseClaimService.submitClaim(orgId, claimId, userId)

        assertEquals(ExpenseClaimStatus.SUBMITTED, response.status)
    }

    @Test
    fun `approveClaim posts journal entry and updates status`() {
        val claimId = UUID.randomUUID()
        val claim =
            ExpenseClaim(
                id = claimId,
                organizationId = orgId,
                employeeId = empId,
                claimDate = LocalDate.now(),
                purpose = "Business Trip",
                status = ExpenseClaimStatus.SUBMITTED,
                reimbursementCurrency = "USD",
                totalReimbursementAmount = BigDecimal("160.00"),
                createdBy = userId,
            )

        val expAccountId = UUID.randomUUID()
        val payAccountId = UUID.randomUUID()

        val expAccount = Account(id = expAccountId, organizationId = orgId, code = "EXP", name = "Expense", type = AccountType.EXPENSE)
        val payAccount = Account(id = payAccountId, organizationId = orgId, code = "PAY", name = "Payable", type = AccountType.LIABILITY)

        whenever(expenseClaimRepository.findById(claimId)).thenReturn(Optional.of(claim))
        whenever(accountRepository.findAllById(any())).thenReturn(listOf(expAccount, payAccount))
        whenever(expenseClaimRepository.save(any<ExpenseClaim>())).thenAnswer { it.arguments[0] as ExpenseClaim }

        val je =
            JournalEntry(
                id = UUID.randomUUID(),
                entryNumber = "JE-001",
                date = LocalDate.now(),
                description = "",
                organizationId = orgId,
                status = JournalEntryStatus.POSTED,
                source = JournalEntrySource.SYSTEM,
                sourceReference = "",
                lines = emptyList(),
                createdBy = userId,
            )
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any())).thenReturn(je)

        val response = expenseClaimService.approveClaim(orgId, claimId, userId, expAccountId, payAccountId)

        assertEquals(ExpenseClaimStatus.APPROVED, response.status)
        assertEquals(je.id, response.journalEntryId)

        verify(journalEntryService).createSystemEntry(
            date = eq(claim.claimDate),
            description = any(),
            organizationId = eq(orgId),
            lines = any(),
            sourceReference = eq("expense_claim:${claim.id}"),
            createdBy = eq(userId),
        )
    }

    @Test
    fun `approveClaim throws exception if not submitted`() {
        val claimId = UUID.randomUUID()
        val claim =
            ExpenseClaim(
                id = claimId,
                organizationId = orgId,
                employeeId = empId,
                claimDate = LocalDate.now(),
                purpose = "Business Trip",
                status = ExpenseClaimStatus.DRAFT,
                reimbursementCurrency = "USD",
                createdBy = userId,
            )
        whenever(expenseClaimRepository.findById(claimId)).thenReturn(Optional.of(claim))

        assertThrows(BusinessRuleException::class.java) {
            expenseClaimService.approveClaim(orgId, claimId, userId, UUID.randomUUID(), UUID.randomUUID())
        }
    }

    @Test
    fun `reimburseClaim posts journal entry and updates status`() {
        val claimId = UUID.randomUUID()
        val claim =
            ExpenseClaim(
                id = claimId,
                organizationId = orgId,
                employeeId = empId,
                claimDate = LocalDate.now(),
                purpose = "Business Trip",
                status = ExpenseClaimStatus.APPROVED,
                reimbursementCurrency = "USD",
                totalReimbursementAmount = BigDecimal("160.00"),
                createdBy = userId,
            )

        val payAccountId = UUID.randomUUID()
        val cashAccountId = UUID.randomUUID()

        val payAccount = Account(id = payAccountId, organizationId = orgId, code = "PAY", name = "Payable", type = AccountType.LIABILITY)
        val cashAccount = Account(id = cashAccountId, organizationId = orgId, code = "CASH", name = "Bank", type = AccountType.ASSET)

        whenever(expenseClaimRepository.findById(claimId)).thenReturn(Optional.of(claim))
        whenever(accountRepository.findAllById(any())).thenReturn(listOf(payAccount, cashAccount))
        whenever(expenseClaimRepository.save(any<ExpenseClaim>())).thenAnswer { it.arguments[0] as ExpenseClaim }

        val je =
            JournalEntry(
                id = UUID.randomUUID(),
                entryNumber = "JE-002",
                date = LocalDate.now(),
                description = "",
                organizationId = orgId,
                status = JournalEntryStatus.POSTED,
                source = JournalEntrySource.SYSTEM,
                sourceReference = "",
                lines = emptyList(),
                createdBy = userId,
            )
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any())).thenReturn(je)

        val response = expenseClaimService.reimburseClaim(orgId, claimId, userId, payAccountId, cashAccountId)

        assertEquals(ExpenseClaimStatus.PAID, response.status)
        assertEquals(je.id, response.paymentJournalEntryId)

        verify(journalEntryService).createSystemEntry(
            date = any(),
            description = any(),
            organizationId = eq(orgId),
            lines = any(),
            sourceReference = eq("expense_claim_payment:${claim.id}"),
            createdBy = eq(userId),
        )
    }

    @Test
    fun `reimburseClaim throws exception if not approved`() {
        val claimId = UUID.randomUUID()
        val claim =
            ExpenseClaim(
                id = claimId,
                organizationId = orgId,
                employeeId = empId,
                claimDate = LocalDate.now(),
                purpose = "Business Trip",
                status = ExpenseClaimStatus.SUBMITTED,
                reimbursementCurrency = "USD",
                createdBy = userId,
            )
        whenever(expenseClaimRepository.findById(claimId)).thenReturn(Optional.of(claim))

        assertThrows(BusinessRuleException::class.java) {
            expenseClaimService.reimburseClaim(orgId, claimId, userId, UUID.randomUUID(), UUID.randomUUID())
        }
    }

    @Test
    fun `submitClaim throws exception if policy limit exceeded`() {
        val categoryId = UUID.randomUUID()
        val claimId = UUID.randomUUID()

        val claim =
            ExpenseClaim(
                id = claimId,
                organizationId = orgId,
                employeeId = empId,
                claimDate = LocalDate.now(),
                purpose = "Business Trip",
                status = ExpenseClaimStatus.DRAFT,
                reimbursementCurrency = "USD",
                createdBy = userId,
            )

        val line =
            ExpenseClaimLine(
                lineNumber = 1,
                expenseDate = LocalDate.now(),
                category = "Meals",
                categoryId = categoryId,
                originalCurrency = "USD",
                originalAmount = BigDecimal("100"),
                reimbursementAmount = BigDecimal("100"),
            )
        claim.lines.add(line)

        val category =
            ExpenseCategory(
                id = categoryId,
                organizationId = orgId,
                name = "Meals",
                expenseAccountId = UUID.randomUUID(),
                policyLimit = BigDecimal("50"),
                limitCurrency = "USD",
                createdBy = userId,
            )

        whenever(expenseClaimRepository.findById(claimId)).thenReturn(Optional.of(claim))
        whenever(expenseCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category))

        val ex =
            assertThrows(BusinessRuleException::class.java) {
                expenseClaimService.submitClaim(orgId, claimId, userId)
            }

        assert(ex.message!!.contains("exceeds policy limit"))
    }
}
