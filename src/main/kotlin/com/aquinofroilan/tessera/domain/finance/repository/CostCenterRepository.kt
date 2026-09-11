package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.CostCenter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CostCenterRepository : JpaRepository<CostCenter, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<CostCenter>
}
