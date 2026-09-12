package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceRepository : JpaRepository<Invoice, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Invoice>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: InvoiceStatus,
    ): List<Invoice>

    fun findByOrganizationIdAndCustomerId(
        organizationId: java.util.UUID,
        customerId: java.util.UUID,
    ): List<Invoice>

    fun findByOrganizationIdAndStatusAndCustomerId(
        organizationId: java.util.UUID,
        status: InvoiceStatus,
        customerId: java.util.UUID,
    ): List<Invoice>

    fun findByOrganizationIdAndStatusIn(
        organizationId: java.util.UUID,
        statuses: List<InvoiceStatus>,
    ): List<Invoice>

    fun countByOrganizationId(organizationId: java.util.UUID): Long

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.organizationId = :organizationId AND i.salespersonId = :salespersonId AND i.date >= :startDate AND i.date <= :endDate AND i.status NOT IN ('DRAFT', 'VOID')",
    )
    fun sumAmountBySalespersonAndPeriod(
        @org.springframework.data.repository.query.Param("organizationId") organizationId: java.util.UUID,
        @org.springframework.data.repository.query.Param("salespersonId") salespersonId: java.util.UUID,
        @org.springframework.data.repository.query.Param("startDate") startDate: java.time.LocalDate,
        @org.springframework.data.repository.query.Param("endDate") endDate: java.time.LocalDate,
    ): java.math.BigDecimal
}
