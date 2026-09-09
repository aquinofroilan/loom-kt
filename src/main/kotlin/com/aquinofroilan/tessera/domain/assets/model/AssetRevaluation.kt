package com.aquinofroilan.tessera.domain.assets.model

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
@Table(name = "asset_revaluations")
@EntityListeners(AuditingEntityListener::class)
data class AssetRevaluation(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "asset_id", columnDefinition = "uuid")
    var assetId: UUID,
    @Column(name = "revaluation_date")
    var revaluationDate: LocalDate,
    @Column(name = "previous_cost")
    var previousCost: BigDecimal,
    @Column(name = "new_cost")
    var newCost: BigDecimal,
    @Column(name = "revaluation_amount")
    var revaluationAmount: BigDecimal,
    @Column(name = "revaluation_account_id", columnDefinition = "uuid")
    var revaluationAccountId: UUID,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    var journalEntryId: UUID? = null,
    var reason: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
)
