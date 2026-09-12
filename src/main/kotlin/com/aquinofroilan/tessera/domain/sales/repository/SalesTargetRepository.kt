package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.SalesTarget
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SalesTargetRepository : JpaRepository<SalesTarget, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<SalesTarget>

    fun findByOrganizationIdAndDepartmentId(
        organizationId: java.util.UUID,
        departmentId: java.util.UUID,
    ): List<SalesTarget>

    fun findByOrganizationId(organizationId: java.util.UUID): List<SalesTarget>
}
