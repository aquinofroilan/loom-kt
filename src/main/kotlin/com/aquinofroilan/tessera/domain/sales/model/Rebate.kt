package com.aquinofroilan.tessera.domain.sales.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class RebateType {
    VOLUME_PERCENTAGE,
    FLAT_AMOUNT_PER_UNIT,
}

@Entity
@Table(name = "rebate_programs")
@EntityListeners(AuditingEntityListener::class)
class RebateProgram(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    var name: String,
    @Enumerated(EnumType.STRING)
    var type: RebateType,
    @Column(name = "customer_id", columnDefinition = "uuid")
    var customerId: java.util.UUID? = null,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: java.util.UUID? = null,
    @Column(name = "rate_value")
    var rateValue: BigDecimal, // Percentage (e.g. 2.5) or amount (e.g. 5.00 per unit)
    @Column(name = "period_start")
    var periodStart: LocalDate,
    @Column(name = "period_end")
    var periodEnd: LocalDate,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)

@Entity
@Table(name = "rebate_accruals")
@EntityListeners(AuditingEntityListener::class)
class RebateAccrual(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "rebate_program_id", columnDefinition = "uuid")
    var rebateProgramId: java.util.UUID,
    @Column(name = "customer_id", columnDefinition = "uuid")
    var customerId: java.util.UUID,
    @Column(name = "invoice_id", columnDefinition = "uuid")
    var invoiceId: java.util.UUID,
    var amount: BigDecimal,
    var currency: String,
    @Column(name = "is_settled")
    var isSettled: Boolean = false,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
