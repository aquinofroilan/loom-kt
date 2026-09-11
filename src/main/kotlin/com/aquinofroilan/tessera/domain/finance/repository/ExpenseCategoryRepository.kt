package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.ExpenseCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ExpenseCategoryRepository : JpaRepository<ExpenseCategory, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<ExpenseCategory>

    fun findByOrganizationIdAndName(
        organizationId: UUID,
        name: String,
    ): ExpenseCategory?
}
