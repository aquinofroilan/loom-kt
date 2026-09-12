package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.event.InvoiceApprovedEvent
import com.aquinofroilan.tessera.domain.sales.model.CreditNote
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteLine
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteStatus
import com.aquinofroilan.tessera.domain.sales.model.RebateAccrual
import com.aquinofroilan.tessera.domain.sales.model.RebateType
import com.aquinofroilan.tessera.domain.sales.repository.CreditNoteRepository
import com.aquinofroilan.tessera.domain.sales.repository.RebateAccrualRepository
import com.aquinofroilan.tessera.domain.sales.repository.RebateProgramRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class RebateService(
    private val rebateProgramRepository: RebateProgramRepository,
    private val rebateAccrualRepository: RebateAccrualRepository,
    private val creditNoteRepository: CreditNoteRepository,
) {
    @EventListener
    @Transactional
    fun onInvoiceApproved(event: InvoiceApprovedEvent) {
        val invoice = event.invoice
        val programs = rebateProgramRepository.findByOrganizationIdAndCustomerId(invoice.organizationId, invoice.customerId)

        for (program in programs) {
            // Check if within period
            if (invoice.date.isBefore(program.periodStart) || invoice.date.isAfter(program.periodEnd)) {
                continue
            }

            // Ensure not already accrued
            if (rebateAccrualRepository.existsByInvoiceIdAndRebateProgramId(invoice.id, program.id)) {
                continue
            }

            var rebateAmount = BigDecimal.ZERO

            if (program.type == RebateType.VOLUME_PERCENTAGE) {
                rebateAmount = invoice.totalAmount.multiply(program.rateValue).divide(BigDecimal("100"))
            } else if (program.type == RebateType.FLAT_AMOUNT_PER_UNIT) {
                // If it applies to a specific product, sum matching invoice lines
                if (program.productId != null) {
                    // Invoice lines don't natively expose productId in Tessera's finance module,
                    // we'll approximate flat amount rebate as rateValue * number of lines for now, or just skip
                    // In a real system, invoice lines would map back to products via sales order.
                    rebateAmount = program.rateValue
                } else {
                    rebateAmount = program.rateValue
                }
            }

            if (rebateAmount > BigDecimal.ZERO) {
                rebateAccrualRepository.save(
                    RebateAccrual(
                        rebateProgramId = program.id,
                        customerId = invoice.customerId,
                        invoiceId = invoice.id,
                        amount = rebateAmount,
                        currency = invoice.currencyCode,
                        organizationId = invoice.organizationId,
                    ),
                )
            }
        }
    }

    @Transactional
    fun settleRebates(
        organizationId: UUID,
        customerId: UUID,
        createdBy: UUID,
    ): CreditNote? {
        val pending = rebateAccrualRepository.findByOrganizationIdAndCustomerIdAndIsSettledFalse(organizationId, customerId)
        if (pending.isEmpty()) return null

        val totalAmount = pending.fold(BigDecimal.ZERO) { acc, rebate -> acc.add(rebate.amount) }
        val currency = pending.first().currency

        val creditNote =
            CreditNote(
                organizationId = organizationId,
                creditNoteNumber = "CN-REBATE-\${System.currentTimeMillis()}",
                customerId = customerId,
                customerName = "Rebate Settlement",
                date = LocalDate.now(),
                currency = currency,
                totalAmount = totalAmount,
                status = CreditNoteStatus.DRAFT,
                reason = "Rebate Settlement",
                createdBy = createdBy,
            )

        val creditNoteLine =
            CreditNoteLine(
                creditNoteId = creditNote.id,
                lineNumber = 1,
                description = "Volume Rebate Settlement",
                quantity = BigDecimal.ONE,
                unitPrice = totalAmount,
                lineTotal = totalAmount,
            )

        creditNote.lines.add(creditNoteLine)
        val saved = creditNoteRepository.save(creditNote)

        pending.forEach {
            it.isSettled = true
            rebateAccrualRepository.save(it)
        }

        return saved
    }
}
