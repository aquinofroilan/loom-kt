package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateExpenseClaimRequest
import com.aquinofroilan.tessera.domain.finance.dto.ExpenseClaimLineResponse
import com.aquinofroilan.tessera.domain.finance.dto.ExpenseClaimResponse
import com.aquinofroilan.tessera.domain.finance.model.ExpenseClaim
import com.aquinofroilan.tessera.domain.finance.model.ExpenseClaimLine
import com.aquinofroilan.tessera.domain.finance.model.ExpenseClaimStatus
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryLine
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.ExpenseClaimRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class ExpenseClaimService(
    private val expenseClaimRepository: ExpenseClaimRepository,
    private val accountRepository: AccountRepository,
    private val journalEntryService: JournalEntryService,
) {
    private val log = LoggerFactory.getLogger(ExpenseClaimService::class.java)

    @Transactional
    fun createClaim(
        organizationId: UUID,
        userId: UUID,
        request: CreateExpenseClaimRequest,
    ): ExpenseClaimResponse {
        val employeeId = request.employeeId ?: throw BusinessRuleException("Employee ID is required")
        val linesRequest = request.lines ?: throw BusinessRuleException("At least one line item is required")
        if (linesRequest.isEmpty()) throw BusinessRuleException("At least one line item is required")

        val claim =
            ExpenseClaim(
                organizationId = organizationId,
                employeeId = employeeId,
                claimDate = request.claimDate!!,
                purpose = request.purpose!!,
                status = ExpenseClaimStatus.DRAFT,
                reimbursementCurrency = request.reimbursementCurrency!!,
                createdBy = userId,
            )

        linesRequest.forEachIndexed { index, lineReq ->
            val reimbursementAmount =
                lineReq.originalAmount!!
                    .multiply(lineReq.exchangeRate)
                    .setScale(4, RoundingMode.HALF_UP)

            val line =
                ExpenseClaimLine(
                    lineNumber = index + 1,
                    expenseDate = lineReq.expenseDate!!,
                    category = lineReq.category!!,
                    description = lineReq.description,
                    originalCurrency = lineReq.originalCurrency!!,
                    originalAmount = lineReq.originalAmount,
                    exchangeRate = lineReq.exchangeRate,
                    reimbursementAmount = reimbursementAmount,
                    projectId = lineReq.projectId,
                    receiptUrl = lineReq.receiptUrl,
                )
            claim.lines.add(line)
        }

        claim.calculateTotal()
        val saved = expenseClaimRepository.save(claim)
        log.info("Created expense claim {} for employee {}", saved.id, employeeId)
        return mapToResponse(saved)
    }

    @Transactional
    fun submitClaim(
        organizationId: UUID,
        claimId: UUID,
        userId: UUID,
    ): ExpenseClaimResponse {
        val claim = getClaim(claimId, organizationId)
        if (claim.status != ExpenseClaimStatus.DRAFT) {
            throw BusinessRuleException("Only draft claims can be submitted")
        }

        claim.status = ExpenseClaimStatus.SUBMITTED
        // In a real scenario, this is where we would integrate with the ApprovalWorkflow engine
        val saved = expenseClaimRepository.save(claim)
        log.info("Submitted expense claim {}", claimId)
        return mapToResponse(saved)
    }

    @Transactional
    fun approveClaim(
        organizationId: UUID,
        claimId: UUID,
        userId: UUID,
        expenseAccountId: UUID,
        payableAccountId: UUID,
    ): ExpenseClaimResponse {
        val claim = getClaim(claimId, organizationId)
        if (claim.status != ExpenseClaimStatus.SUBMITTED) {
            throw BusinessRuleException("Only submitted claims can be approved")
        }

        val accounts = accountRepository.findAllById(listOf(expenseAccountId, payableAccountId)).associateBy { it.id }
        val expAccount = accounts[expenseAccountId] ?: throw BusinessRuleException("Expense account not found")
        val payAccount = accounts[payableAccountId] ?: throw BusinessRuleException("Payable account not found")

        val lines = mutableListOf<JournalEntryLine>()

        // Debit Expense
        lines.add(
            JournalEntryLine(
                accountId = expAccount.id,
                accountCode = expAccount.code,
                accountName = expAccount.name,
                debit = claim.totalReimbursementAmount,
                credit = BigDecimal.ZERO,
            ),
        )
        // Credit Payable
        lines.add(
            JournalEntryLine(
                accountId = payAccount.id,
                accountCode = payAccount.code,
                accountName = payAccount.name,
                debit = BigDecimal.ZERO,
                credit = claim.totalReimbursementAmount,
            ),
        )

        val je =
            journalEntryService.createSystemEntry(
                date = claim.claimDate,
                description = "Expense Claim Approval - ${claim.purpose}",
                organizationId = organizationId,
                lines = lines,
                sourceReference = "expense_claim:${claim.id}",
                createdBy = userId,
            )

        claim.status = ExpenseClaimStatus.APPROVED
        claim.journalEntryId = je.id
        val saved = expenseClaimRepository.save(claim)
        log.info("Approved expense claim {} and posted journal entry {}", claimId, je.id)
        return mapToResponse(saved)
    }

    @Transactional
    fun rejectClaim(
        organizationId: UUID,
        claimId: UUID,
        userId: UUID,
    ): ExpenseClaimResponse {
        val claim = getClaim(claimId, organizationId)
        if (claim.status != ExpenseClaimStatus.SUBMITTED) {
            throw BusinessRuleException("Only submitted claims can be rejected")
        }

        claim.status = ExpenseClaimStatus.REJECTED
        val saved = expenseClaimRepository.save(claim)
        log.info("Rejected expense claim {}", claimId)
        return mapToResponse(saved)
    }

    fun getClaim(
        claimId: UUID,
        organizationId: UUID,
    ): ExpenseClaim =
        expenseClaimRepository
            .findById(claimId)
            .orElseThrow {
                ResourceNotFoundException("ExpenseClaim not found: $claimId")
            }.also {
                if (it.organizationId != organizationId) {
                    throw BusinessRuleException("Expense claim does not belong to organization")
                }
            }

    fun getClaimResponse(
        claimId: UUID,
        organizationId: UUID,
    ): ExpenseClaimResponse = mapToResponse(getClaim(claimId, organizationId))

    fun listClaims(
        organizationId: UUID,
        employeeId: UUID? = null,
    ): List<ExpenseClaimResponse> {
        val claims =
            if (employeeId != null) {
                expenseClaimRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
            } else {
                expenseClaimRepository.findByOrganizationId(organizationId)
            }
        return claims.map { mapToResponse(it) }
    }

    @Transactional
    fun reimburseClaim(
        organizationId: UUID,
        claimId: UUID,
        userId: UUID,
        payableAccountId: UUID,
        cashAccountId: UUID,
    ): ExpenseClaimResponse {
        val claim = getClaim(claimId, organizationId)
        if (claim.status != ExpenseClaimStatus.APPROVED) {
            throw BusinessRuleException("Only approved claims can be reimbursed")
        }

        val accounts = accountRepository.findAllById(listOf(payableAccountId, cashAccountId)).associateBy { it.id }
        val payAccount = accounts[payableAccountId] ?: throw BusinessRuleException("Payable account not found")
        val cashAccount = accounts[cashAccountId] ?: throw BusinessRuleException("Cash/Bank account not found")

        val lines = mutableListOf<JournalEntryLine>()

        // Debit Payable
        lines.add(
            JournalEntryLine(
                accountId = payAccount.id,
                accountCode = payAccount.code,
                accountName = payAccount.name,
                debit = claim.totalReimbursementAmount,
                credit = BigDecimal.ZERO,
            ),
        )
        // Credit Cash
        lines.add(
            JournalEntryLine(
                accountId = cashAccount.id,
                accountCode = cashAccount.code,
                accountName = cashAccount.name,
                debit = BigDecimal.ZERO,
                credit = claim.totalReimbursementAmount,
            ),
        )

        val je =
            journalEntryService.createSystemEntry(
                date = LocalDate.now(),
                description = "Expense Claim Reimbursement - ${claim.purpose}",
                organizationId = organizationId,
                lines = lines,
                sourceReference = "expense_claim_payment:${claim.id}",
                createdBy = userId,
            )

        claim.status = ExpenseClaimStatus.PAID
        claim.paymentJournalEntryId = je.id
        val saved = expenseClaimRepository.save(claim)
        log.info("Reimbursed expense claim {} and posted journal entry {}", claimId, je.id)
        return mapToResponse(saved)
    }

    private fun mapToResponse(claim: ExpenseClaim): ExpenseClaimResponse =
        ExpenseClaimResponse(
            id = claim.id,
            organizationId = claim.organizationId,
            employeeId = claim.employeeId,
            claimDate = claim.claimDate.toString(),
            purpose = claim.purpose,
            status = claim.status,
            reimbursementCurrency = claim.reimbursementCurrency,
            totalReimbursementAmount = claim.totalReimbursementAmount,
            workflowInstanceId = claim.workflowInstanceId,
            journalEntryId = claim.journalEntryId,
            paymentJournalEntryId = claim.paymentJournalEntryId,
            createdBy = claim.createdBy,
            createdAt = claim.createdAt?.toString() ?: "",
            updatedAt = claim.updatedAt?.toString(),
            lines =
                claim.lines.map { line ->
                    ExpenseClaimLineResponse(
                        id = line.id,
                        lineNumber = line.lineNumber,
                        expenseDate = line.expenseDate.toString(),
                        category = line.category,
                        description = line.description,
                        originalCurrency = line.originalCurrency,
                        originalAmount = line.originalAmount,
                        exchangeRate = line.exchangeRate,
                        reimbursementAmount = line.reimbursementAmount,
                        projectId = line.projectId,
                        receiptUrl = line.receiptUrl,
                    )
                },
        )
}
