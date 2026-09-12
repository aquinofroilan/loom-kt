package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.CommissionAccrual
import com.aquinofroilan.tessera.domain.sales.model.CommissionAssignment
import com.aquinofroilan.tessera.domain.sales.model.CommissionPlan
import com.aquinofroilan.tessera.domain.sales.model.CommissionStatement
import com.aquinofroilan.tessera.domain.sales.model.CommissionStatementStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommissionPlanRepository : JpaRepository<CommissionPlan, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<CommissionPlan>
}

@Repository
interface CommissionAssignmentRepository : JpaRepository<CommissionAssignment, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<CommissionAssignment>
}

@Repository
interface CommissionStatementRepository : JpaRepository<CommissionStatement, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<CommissionStatement>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: CommissionStatementStatus,
    ): List<CommissionStatement>
}

@Repository
interface CommissionAccrualRepository : JpaRepository<CommissionAccrual, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeIdAndStatementIdIsNull(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<CommissionAccrual>

    fun existsByInvoiceIdAndEmployeeId(
        invoiceId: java.util.UUID,
        employeeId: java.util.UUID,
    ): Boolean
}
