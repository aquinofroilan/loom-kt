package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.Payslip
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PayslipRepository : JpaRepository<Payslip, java.util.UUID> {
    fun existsByPayrollRunIdAndEmployeeId(
        payrollRunId: java.util.UUID,
        employeeId: java.util.UUID,
    ): Boolean
}
