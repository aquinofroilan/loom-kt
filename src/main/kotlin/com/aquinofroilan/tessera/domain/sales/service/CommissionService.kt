package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.event.InvoiceApprovedEvent
import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.sales.model.CommissionAccrual
import com.aquinofroilan.tessera.domain.sales.model.CommissionPlanType
import com.aquinofroilan.tessera.domain.sales.model.CommissionStatement
import com.aquinofroilan.tessera.domain.sales.model.CommissionStatementStatus
import com.aquinofroilan.tessera.domain.sales.repository.CommissionAccrualRepository
import com.aquinofroilan.tessera.domain.sales.repository.CommissionAssignmentRepository
import com.aquinofroilan.tessera.domain.sales.repository.CommissionPlanRepository
import com.aquinofroilan.tessera.domain.sales.repository.CommissionStatementRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class CommissionService(
    private val planRepository: CommissionPlanRepository,
    private val assignmentRepository: CommissionAssignmentRepository,
    private val accrualRepository: CommissionAccrualRepository,
    private val statementRepository: CommissionStatementRepository,
) {
    @EventListener
    @Transactional
    fun onInvoiceApproved(event: InvoiceApprovedEvent) {
        accrueFromInvoice(event.invoice)
    }

    @Transactional
    fun accrueFromInvoice(invoice: Invoice) {
        val salespersonId = invoice.salespersonId ?: return

        // Ensure we don't accrue multiple times for the same invoice/employee
        if (accrualRepository.existsByInvoiceIdAndEmployeeId(invoice.id, salespersonId)) {
            return
        }

        val assignments = assignmentRepository.findByOrganizationIdAndEmployeeId(invoice.organizationId, salespersonId)
        if (assignments.isEmpty()) return

        // Take the first plan for simplicity
        val assignment = assignments.first()
        val plan = planRepository.findById(assignment.planId).orElse(null) ?: return

        var commissionAmount = BigDecimal.ZERO
        if (plan.type == CommissionPlanType.FLAT_RATE && plan.flatRatePercentage != null) {
            commissionAmount = invoice.totalAmount.multiply(plan.flatRatePercentage).divide(BigDecimal("100"))
        } else if (plan.type == CommissionPlanType.TIERED) {
            // Placeholder for tiered logic
            commissionAmount = invoice.totalAmount.multiply(BigDecimal("0.02"))
        }

        if (commissionAmount > BigDecimal.ZERO) {
            accrualRepository.save(
                CommissionAccrual(
                    employeeId = salespersonId,
                    invoiceId = invoice.id,
                    amount = commissionAmount,
                    currency = invoice.currencyCode,
                    organizationId = invoice.organizationId,
                ),
            )
        }
    }

    @Transactional
    fun generateStatement(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ): CommissionStatement {
        val pendingAccruals = accrualRepository.findByOrganizationIdAndEmployeeIdAndStatementIdIsNull(organizationId, employeeId)
        if (pendingAccruals.isEmpty()) {
            throw IllegalArgumentException("No pending accruals for employee \$employeeId")
        }

        val totalAmount = pendingAccruals.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)

        val statement =
            statementRepository.save(
                CommissionStatement(
                    employeeId = employeeId,
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    totalAmount = totalAmount,
                    organizationId = organizationId,
                    status = CommissionStatementStatus.DRAFT,
                ),
            )

        pendingAccruals.forEach {
            it.statementId = statement.id
            accrualRepository.save(it)
        }

        return statement
    }
}
