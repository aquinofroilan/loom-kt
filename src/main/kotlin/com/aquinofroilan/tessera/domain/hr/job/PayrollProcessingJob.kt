package com.aquinofroilan.tessera.domain.hr.job

import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.model.PayPeriod
import com.aquinofroilan.tessera.domain.hr.model.PayrollRun
import com.aquinofroilan.tessera.domain.hr.model.PayrollRunLine
import com.aquinofroilan.tessera.domain.hr.model.PayrollRunStatus
import com.aquinofroilan.tessera.domain.hr.model.Payslip
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeCompensationRepository
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeRepository
import com.aquinofroilan.tessera.domain.hr.repository.PayrollRunRepository
import com.aquinofroilan.tessera.domain.hr.repository.PayslipRepository
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import jakarta.annotation.PostConstruct
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.scheduling.JobScheduler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Component
class PayrollProcessingJob(
    private val organizationRepository: OrganizationRepository,
    private val employeeRepository: EmployeeRepository,
    private val compensationRepository: EmployeeCompensationRepository,
    private val payrollRunRepository: PayrollRunRepository,
    private val payslipRepository: PayslipRepository,
    private val jobScheduler: JobScheduler,
) {
    private val log = LoggerFactory.getLogger(PayrollProcessingJob::class.java)

    @Value("\${tessera.hr.payroll-processing.cron:0 0 1,15 * *}")
    private lateinit var cronExpression: String

    @PostConstruct
    fun scheduleJob() {
        jobScheduler.scheduleRecurrently("payroll-processing-job", cronExpression) {
            execute()
        }
    }

    @Job(name = "Payroll Processing Job")
    @Transactional
    fun execute() {
        val orgs = organizationRepository.findAll().filter { it.isActive }
        val periodEnd = LocalDate.now()
        val periodStart = if (periodEnd.dayOfMonth <= 15) periodEnd.withDayOfMonth(1) else periodEnd.withDayOfMonth(16)

        for (org in orgs) {
            try {
                processPayrollForOrg(org.uuid, periodStart, periodEnd)
            } catch (e: Exception) {
                log.error("Failed to process payroll for organization \${org.uuid}", e)
            }
        }
    }

    private fun processPayrollForOrg(
        orgId: java.util.UUID,
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ) {
        val existingRun =
            payrollRunRepository.findByOrganizationId(orgId).firstOrNull {
                it.periodStart == periodStart && it.periodEnd == periodEnd
            }

        if (existingRun != null && existingRun.status != PayrollRunStatus.DRAFT) {
            log.info("Payroll run for org \$orgId and period \$periodStart to \$periodEnd already exists and is not a draft. Skipping.")
            return
        }

        val activeEmployees = employeeRepository.findByOrganizationIdAndStatus(orgId, EmploymentStatus.ACTIVE)
        if (activeEmployees.isEmpty()) return

        val runId = existingRun?.id ?: java.util.UUID.randomUUID()
        val lines = mutableListOf<PayrollRunLine>()
        var totalGross = BigDecimal.ZERO
        var runCurrency = "USD"

        // Use a static system user ID for the job, or the org ID
        val systemUserId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

        for ((index, employee) in activeEmployees.withIndex()) {
            val compensations = compensationRepository.findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(orgId, employee.id)
            val currentComp = compensations.firstOrNull { !it.effectiveDate.isAfter(periodEnd) } ?: continue

            runCurrency = currentComp.currency

            val grossPay = calculateGrossPay(currentComp.payRate, currentComp.payPeriod)
            val taxDeductions = grossPay.multiply(BigDecimal("0.20")) // Simple 20% tax deduction for now
            val netPay = grossPay.subtract(taxDeductions)

            totalGross = totalGross.add(grossPay)

            lines.add(
                PayrollRunLine(
                    lineNumber = index + 1,
                    employeeId = employee.id,
                    employeeNumber = employee.employeeNumber,
                    employeeName = "\${employee.firstName} \${employee.lastName}",
                    compensationId = currentComp.id,
                    grossAmount = grossPay,
                ),
            )

            // Create Payslip if it doesn't exist
            if (!payslipRepository.existsByPayrollRunIdAndEmployeeId(runId, employee.id)) {
                payslipRepository.save(
                    Payslip(
                        employeeId = employee.id,
                        payrollRunId = runId,
                        periodStart = periodStart,
                        periodEnd = periodEnd,
                        grossPay = grossPay,
                        taxDeductions = taxDeductions,
                        netPay = netPay,
                        currency = currentComp.currency,
                        organizationId = orgId,
                        isDirectDepositQueued = true, // Enqueue for direct deposit automatically
                    ),
                )
            }
        }

        if (lines.isEmpty()) return

        val payrollRun =
            existingRun?.apply {
                this.lines = lines
                this.totalGross = totalGross
                this.currency = runCurrency
            } ?: PayrollRun(
                id = runId,
                runNumber = "PR-\${periodEnd.year}\${periodEnd.monthValue}\${periodEnd.dayOfMonth}",
                periodStart = periodStart,
                periodEnd = periodEnd,
                payDate = periodEnd,
                organizationId = orgId,
                status = PayrollRunStatus.DRAFT,
                lines = lines,
                totalGross = totalGross,
                currency = runCurrency,
                createdBy = systemUserId,
            )

        payrollRunRepository.save(payrollRun)
    }

    private fun calculateGrossPay(
        payRate: BigDecimal,
        payPeriod: PayPeriod,
    ): BigDecimal =
        when (payPeriod) {
            PayPeriod.ANNUAL -> payRate.divide(BigDecimal("24"), 2, RoundingMode.HALF_UP) // Bi-monthly
            PayPeriod.MONTHLY -> payRate.divide(BigDecimal("2"), 2, RoundingMode.HALF_UP) // Bi-monthly
            PayPeriod.HOURLY -> payRate.multiply(BigDecimal("80")) // Assume 80 hours per period for hourly
        }
}
