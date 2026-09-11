package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.CashAdvance
import com.aquinofroilan.tessera.domain.finance.model.CashAdvanceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CashAdvanceRepository : JpaRepository<CashAdvance, UUID> {
    fun findByOrganizationIdAndEmployeeIdAndStatusIn(
        organizationId: UUID,
        employeeId: UUID,
        statuses: List<CashAdvanceStatus>,
    ): List<CashAdvance>
}
