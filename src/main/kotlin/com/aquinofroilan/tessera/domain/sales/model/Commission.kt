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

enum class CommissionPlanType {
    FLAT_RATE,
    TIERED,
}

@Entity
@Table(name = "commission_plans")
@EntityListeners(AuditingEntityListener::class)
class CommissionPlan(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    var name: String,
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    var type: CommissionPlanType,
    @Column(name = "flat_rate_percentage")
    var flatRatePercentage: BigDecimal? = null, // e.g. 5.0 for 5%
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)

@Entity
@Table(name = "commission_assignments")
@EntityListeners(AuditingEntityListener::class)
class CommissionAssignment(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "plan_id", columnDefinition = "uuid")
    var planId: java.util.UUID,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)

enum class CommissionStatementStatus {
    DRAFT,
    APPROVED,
    PAID,
}

@Entity
@Table(name = "commission_statements")
@EntityListeners(AuditingEntityListener::class)
class CommissionStatement(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "period_start")
    var periodStart: LocalDate,
    @Column(name = "period_end")
    var periodEnd: LocalDate,
    @Column(name = "total_amount")
    var totalAmount: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    var status: CommissionStatementStatus = CommissionStatementStatus.DRAFT,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "payroll_run_id", columnDefinition = "uuid")
    var payrollRunId: java.util.UUID? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)

@Entity
@Table(name = "commission_accruals")
@EntityListeners(AuditingEntityListener::class)
class CommissionAccrual(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "statement_id", columnDefinition = "uuid")
    var statementId: java.util.UUID? = null,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "invoice_id", columnDefinition = "uuid")
    var invoiceId: java.util.UUID,
    var amount: BigDecimal,
    var currency: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
