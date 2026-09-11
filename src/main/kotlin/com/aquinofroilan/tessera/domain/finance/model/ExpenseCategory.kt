package com.aquinofroilan.tessera.domain.finance.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "expense_categories")
@EntityListeners(AuditingEntityListener::class)
class ExpenseCategory(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    var name: String,
    var description: String? = null,
    @Column(name = "expense_account_id", columnDefinition = "uuid")
    var expenseAccountId: UUID,
    @Column(name = "policy_limit")
    var policyLimit: BigDecimal? = null,
    @Column(name = "limit_currency")
    var limitCurrency: String? = null,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
