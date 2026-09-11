package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.Budget
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BudgetRepository : JpaRepository<Budget, UUID> {
    fun findByOrganizationIdAndFiscalYearId(
        organizationId: UUID,
        fiscalYearId: UUID,
    ): List<Budget>
}
