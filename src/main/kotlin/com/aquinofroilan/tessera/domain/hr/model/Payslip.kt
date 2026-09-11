package com.aquinofroilan.tessera.domain.hr.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payslips")
@EntityListeners(AuditingEntityListener::class)
class Payslip(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "payroll_run_id", columnDefinition = "uuid")
    var payrollRunId: java.util.UUID,
    @Column(name = "period_start")
    var periodStart: LocalDate,
    @Column(name = "period_end")
    var periodEnd: LocalDate,
    @Column(name = "gross_pay")
    var grossPay: BigDecimal,
    @Column(name = "tax_deductions")
    var taxDeductions: BigDecimal,
    @Column(name = "net_pay")
    var netPay: BigDecimal,
    var currency: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "is_direct_deposit_queued")
    var isDirectDepositQueued: Boolean = false,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
