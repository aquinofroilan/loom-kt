package com.aquinofroilan.tessera.domain.hr.job

import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.model.EmployeeCompensation
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.model.PayPeriod
import com.aquinofroilan.tessera.domain.hr.model.PayrollRun
import com.aquinofroilan.tessera.domain.hr.model.Payslip
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeCompensationRepository
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeRepository
import com.aquinofroilan.tessera.domain.hr.repository.PayrollRunRepository
import com.aquinofroilan.tessera.domain.hr.repository.PayslipRepository
import com.aquinofroilan.tessera.domain.organization.model.Organizations
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import org.jobrunr.scheduling.JobScheduler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class PayrollProcessingJobTest {
    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var compensationRepository: EmployeeCompensationRepository

    @Mock
    private lateinit var payrollRunRepository: PayrollRunRepository

    @Mock
    private lateinit var payslipRepository: PayslipRepository

    @Mock
    private lateinit var jobScheduler: JobScheduler

    @InjectMocks
    private lateinit var job: PayrollProcessingJob

    @Test
    fun `should process payroll and generate payslips`() {
        val orgId = UUID.randomUUID()
        val employeeId = UUID.randomUUID()

        val org =
            Organizations(
                uuid = orgId,
                orgSlug = "test-org",
                name = "Test Org",
                legalName = "Test Org Inc",
                tradeName = "Test Org",
                baseCurrency = "USD",
                isActive = true,
                fiscalYearStart = LocalDateTime.now(),
                timezone = "UTC",
            )

        val employee =
            Employee(
                id = employeeId,
                employeeNumber = "EMP-001",
                firstName = "John",
                lastName = "Doe",
                hireDate = LocalDate.now().minusYears(1),
                organizationId = orgId,
                status = EmploymentStatus.ACTIVE,
            )

        val compensation =
            EmployeeCompensation(
                id = UUID.randomUUID(),
                employeeId = employeeId,
                payRate = BigDecimal("120000"), // Annual
                currency = "USD",
                payPeriod = PayPeriod.ANNUAL,
                effectiveDate = LocalDate.now().minusMonths(6),
                organizationId = orgId,
                createdBy = UUID.randomUUID(),
            )

        `when`(organizationRepository.findAll()).thenReturn(listOf(org))
        `when`(payrollRunRepository.findByOrganizationId(orgId)).thenReturn(emptyList())
        `when`(employeeRepository.findByOrganizationIdAndStatus(orgId, EmploymentStatus.ACTIVE)).thenReturn(listOf(employee))
        `when`(
            compensationRepository.findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(eq(orgId), eq(employeeId)),
        ).thenReturn(listOf(compensation))
        `when`(payslipRepository.existsByPayrollRunIdAndEmployeeId(any(), eq(employeeId))).thenReturn(false)

        job.execute()

        val payslipCaptor = argumentCaptor<Payslip>()
        verify(payslipRepository).save(payslipCaptor.capture())

        val payslip = payslipCaptor.firstValue
        assertEquals(employeeId, payslip.employeeId)
        assertTrue(BigDecimal("5000.00").compareTo(payslip.grossPay) == 0) // 120000 / 24
        assertTrue(BigDecimal("1000.00").compareTo(payslip.taxDeductions) == 0) // 5000 * 0.20
        assertTrue(BigDecimal("4000.00").compareTo(payslip.netPay) == 0)
        assertTrue(payslip.isDirectDepositQueued)

        val runCaptor = argumentCaptor<PayrollRun>()
        verify(payrollRunRepository).save(runCaptor.capture())

        val run = runCaptor.firstValue
        assertEquals(orgId, run.organizationId)
        assertTrue(BigDecimal("5000.00").compareTo(run.totalGross) == 0)
        assertEquals(1, run.lines.size)
    }
}
