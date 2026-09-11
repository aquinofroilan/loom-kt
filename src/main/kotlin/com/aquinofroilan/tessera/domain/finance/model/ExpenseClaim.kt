package com.aquinofroilan.tessera.domain.finance.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "expense_claims")
@EntityListeners(AuditingEntityListener::class)
data class ExpenseClaim(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: UUID,
    @Column(name = "claim_date")
    var claimDate: LocalDate,
    var purpose: String,
    @Enumerated(EnumType.STRING)
    var status: ExpenseClaimStatus = ExpenseClaimStatus.DRAFT,
    @Column(name = "reimbursement_currency")
    var reimbursementCurrency: String,
    @Column(name = "total_reimbursement_amount")
    var totalReimbursementAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "workflow_instance_id", columnDefinition = "uuid")
    var workflowInstanceId: UUID? = null,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    var journalEntryId: UUID? = null,
    @Column(name = "payment_journal_entry_id", columnDefinition = "uuid")
    var paymentJournalEntryId: UUID? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "claim_id")
    @OrderBy("lineNumber ASC")
    var lines: MutableList<ExpenseClaimLine> = mutableListOf(),
) {
    fun calculateTotal() {
        totalReimbursementAmount =
            lines.fold(BigDecimal.ZERO) { acc, line ->
                acc.add(line.reimbursementAmount)
            }
    }
}
