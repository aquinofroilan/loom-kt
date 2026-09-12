package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.RebateAccrual
import com.aquinofroilan.tessera.domain.sales.model.RebateProgram
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RebateProgramRepository : JpaRepository<RebateProgram, java.util.UUID> {
    fun findByOrganizationIdAndCustomerId(
        organizationId: java.util.UUID,
        customerId: java.util.UUID,
    ): List<RebateProgram>
}

@Repository
interface RebateAccrualRepository : JpaRepository<RebateAccrual, java.util.UUID> {
    fun findByOrganizationIdAndCustomerIdAndIsSettledFalse(
        organizationId: java.util.UUID,
        customerId: java.util.UUID,
    ): List<RebateAccrual>

    fun existsByInvoiceIdAndRebateProgramId(
        invoiceId: java.util.UUID,
        rebateProgramId: java.util.UUID,
    ): Boolean
}
