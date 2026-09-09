package com.aquinofroilan.tessera.domain.assets.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "asset_transfers")
@EntityListeners(AuditingEntityListener::class)
data class AssetTransfer(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "asset_id", columnDefinition = "uuid")
    var assetId: UUID,
    @Column(name = "transfer_date")
    var transferDate: LocalDate,
    @Column(name = "from_location")
    var fromLocation: String? = null,
    @Column(name = "to_location")
    var toLocation: String,
    var reason: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
)
