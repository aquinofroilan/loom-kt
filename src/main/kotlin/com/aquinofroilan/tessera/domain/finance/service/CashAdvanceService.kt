package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CashAdvanceResponse
import com.aquinofroilan.tessera.domain.finance.dto.IssueCashAdvanceRequest
import com.aquinofroilan.tessera.domain.finance.model.CashAdvance
import com.aquinofroilan.tessera.domain.finance.model.CashAdvanceStatus
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryLine
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.CashAdvanceRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class CashAdvanceService(
    private val cashAdvanceRepository: CashAdvanceRepository,
    private val accountRepository: AccountRepository,
    private val journalEntryService: JournalEntryService,
) {
    private val log = LoggerFactory.getLogger(CashAdvanceService::class.java)

    @Transactional
    fun issueAdvance(
        organizationId: UUID,
        userId: UUID,
        request: IssueCashAdvanceRequest,
    ): CashAdvanceResponse {
        val accounts = accountRepository.findAllById(listOf(request.cashAccountId!!, request.advanceAccountId!!)).associateBy { it.id }
        val cashAccount = accounts[request.cashAccountId] ?: throw BusinessRuleException("Cash account not found")
        val advanceAccount = accounts[request.advanceAccountId] ?: throw BusinessRuleException("Advance receivable account not found")

        val advance =
            CashAdvance(
                organizationId = organizationId,
                employeeId = request.employeeId!!,
                date = request.date!!,
                purpose = request.purpose!!,
                amount = request.amount!!,
                currency = request.currency!!,
                status = CashAdvanceStatus.ISSUED,
                createdBy = userId,
            )

        val saved = cashAdvanceRepository.save(advance)

        // Post JE: Debit Advance Receivable, Credit Cash
        val lines =
            listOf(
                JournalEntryLine(
                    accountId = advanceAccount.id,
                    accountCode = advanceAccount.code,
                    accountName = advanceAccount.name,
                    debit = request.amount,
                    credit = BigDecimal.ZERO,
                ),
                JournalEntryLine(
                    accountId = cashAccount.id,
                    accountCode = cashAccount.code,
                    accountName = cashAccount.name,
                    debit = BigDecimal.ZERO,
                    credit = request.amount,
                ),
            )

        val je =
            journalEntryService.createSystemEntry(
                date = request.date,
                description = "Cash Advance - ${request.purpose}",
                organizationId = organizationId,
                lines = lines,
                sourceReference = "cash_advance:${saved.id}",
                createdBy = userId,
            )

        saved.journalEntryId = je.id
        cashAdvanceRepository.save(saved)

        log.info("Issued cash advance {} for employee {}", saved.id, request.employeeId)
        return mapToResponse(saved)
    }

    fun getOutstandingAdvances(
        organizationId: UUID,
        employeeId: UUID,
    ): List<CashAdvanceResponse> {
        val advances =
            cashAdvanceRepository.findByOrganizationIdAndEmployeeIdAndStatusIn(
                organizationId,
                employeeId,
                listOf(CashAdvanceStatus.ISSUED, CashAdvanceStatus.PARTIALLY_RECONCILED),
            )
        return advances.map { mapToResponse(it) }
    }

    private fun mapToResponse(advance: CashAdvance): CashAdvanceResponse =
        CashAdvanceResponse(
            id = advance.id,
            organizationId = advance.organizationId,
            employeeId = advance.employeeId,
            date = advance.date.toString(),
            purpose = advance.purpose,
            amount = advance.amount,
            currency = advance.currency,
            status = advance.status,
            journalEntryId = advance.journalEntryId,
            createdBy = advance.createdBy,
            createdAt = advance.createdAt?.toString() ?: "",
            updatedAt = advance.updatedAt?.toString(),
        )
}
