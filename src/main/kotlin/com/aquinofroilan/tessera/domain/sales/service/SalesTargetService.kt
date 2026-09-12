package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.repository.InvoiceRepository
import com.aquinofroilan.tessera.domain.sales.model.SalesTarget
import com.aquinofroilan.tessera.domain.sales.repository.SalesTargetRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

data class SalesTargetAttainmentReport(
    val target: SalesTarget,
    val actualAmount: BigDecimal,
    val attainmentPercentage: BigDecimal,
)

@Service
class SalesTargetService(
    private val salesTargetRepository: SalesTargetRepository,
    private val invoiceRepository: InvoiceRepository,
) {
    fun getAttainmentReportForEmployee(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<SalesTargetAttainmentReport> {
        val targets = salesTargetRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
        return targets.map { target ->
            val actual =
                invoiceRepository.sumAmountBySalespersonAndPeriod(
                    organizationId,
                    employeeId,
                    target.periodStart,
                    target.periodEnd,
                )

            val percentage =
                if (target.targetAmount > BigDecimal.ZERO) {
                    actual.multiply(BigDecimal("100")).divide(target.targetAmount, 2, java.math.RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }

            SalesTargetAttainmentReport(
                target = target,
                actualAmount = actual,
                attainmentPercentage = percentage,
            )
        }
    }
}
