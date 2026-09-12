package com.aquinofroilan.tessera.domain.procurement.repository

import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrder
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseOrderRepository : JpaRepository<PurchaseOrder, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<PurchaseOrder>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: PurchaseOrderStatus,
    ): List<PurchaseOrder>

    fun findByOrganizationIdAndVendorId(
        organizationId: java.util.UUID,
        vendorId: java.util.UUID,
    ): List<PurchaseOrder>

    fun findByOrganizationIdAndStatusAndVendorId(
        organizationId: java.util.UUID,
        status: PurchaseOrderStatus,
        vendorId: java.util.UUID,
    ): List<PurchaseOrder>

    fun countByOrganizationId(organizationId: java.util.UUID): Long

    @org.springframework.data.jpa.repository.Query(
        """
        SELECT SUM(l.quantity - l.receivedQuantity) 
        FROM PurchaseOrder po 
        JOIN po.lines l 
        WHERE po.organizationId = :organizationId 
          AND l.productId = :productId 
          AND po.warehouseId = :warehouseId 
          AND po.status IN ('APPROVED', 'PARTIALLY_RECEIVED')
    """,
    )
    fun getPendingIncomingQuantity(
        @org.springframework.data.repository.query.Param("organizationId") organizationId: java.util.UUID,
        @org.springframework.data.repository.query.Param("productId") productId: java.util.UUID,
        @org.springframework.data.repository.query.Param("warehouseId") warehouseId: java.util.UUID,
    ): java.math.BigDecimal?

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po WHERE po.organizationId = :organizationId AND po.orderDate >= :startDate",
    )
    fun sumPurchaseVolumeSince(
        @org.springframework.data.repository.query.Param("organizationId") organizationId: java.util.UUID,
        @org.springframework.data.repository.query.Param("startDate") startDate: java.time.LocalDate,
    ): java.math.BigDecimal
}
