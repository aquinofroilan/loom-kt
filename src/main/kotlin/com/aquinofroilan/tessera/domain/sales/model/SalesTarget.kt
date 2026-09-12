package com.aquinofroilan.tessera.domain.sales.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "sales_targets")
@EntityListeners(AuditingEntityListener::class)
class SalesTarget(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID? = null,
    @Column(name = "department_id", columnDefinition = "uuid")
    var departmentId: java.util.UUID? = null,
    @Column(name = "period_start")
    var periodStart: LocalDate,
    @Column(name = "period_end")
    var periodEnd: LocalDate,
    @Column(name = "target_amount")
    var targetAmount: BigDecimal,
    var currency: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
