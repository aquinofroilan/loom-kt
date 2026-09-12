package com.aquinofroilan.tessera.domain.inventory.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "warehouse_zones")
@EntityListeners(AuditingEntityListener::class)
class WarehouseZone(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: java.util.UUID,
    var code: String,
    var name: String,
    var description: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)

@Entity
@Table(name = "warehouse_bins")
@EntityListeners(AuditingEntityListener::class)
class WarehouseBin(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: java.util.UUID,
    @Column(name = "zone_id", columnDefinition = "uuid")
    var zoneId: java.util.UUID? = null,
    var code: String,
    var name: String,
    var description: String? = null,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
