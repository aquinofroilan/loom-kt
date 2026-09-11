package com.aquinofroilan.tessera.domain.finance.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class CashAdvanceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_RECONCILED,
    RECONCILED,
}

@Entity
@Table(name = "cash_advances")
@EntityListeners(AuditingEntityListener::class)
class CashAdvance(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: UUID,
    var date: LocalDate,
    var purpose: String,
    var amount: BigDecimal,
    var currency: String,
    @Enumerated(EnumType.STRING)
    var status: CashAdvanceStatus = CashAdvanceStatus.DRAFT,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    var journalEntryId: UUID? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
