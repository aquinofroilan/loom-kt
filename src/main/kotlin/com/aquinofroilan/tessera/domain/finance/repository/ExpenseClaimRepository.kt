package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.ExpenseClaim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ExpenseClaimRepository : JpaRepository<ExpenseClaim, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<ExpenseClaim>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: UUID,
        employeeId: UUID,
    ): List<ExpenseClaim>
}
