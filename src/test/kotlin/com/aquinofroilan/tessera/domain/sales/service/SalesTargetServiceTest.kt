package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.repository.InvoiceRepository
import com.aquinofroilan.tessera.domain.sales.model.SalesTarget
import com.aquinofroilan.tessera.domain.sales.repository.SalesTargetRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class SalesTargetServiceTest {
    @Mock
    private lateinit var salesTargetRepository: SalesTargetRepository

    @Mock
    private lateinit var invoiceRepository: InvoiceRepository

    @InjectMocks
    private lateinit var service: SalesTargetService

    @Test
    fun `should calculate attainment accurately`() {
        val orgId = UUID.randomUUID()
        val empId = UUID.randomUUID()

        val target =
            SalesTarget(
                employeeId = empId,
                periodStart = LocalDate.of(2026, 1, 1),
                periodEnd = LocalDate.of(2026, 3, 31),
                targetAmount = BigDecimal("100000.00"),
                currency = "USD",
                organizationId = orgId,
            )

        `when`(salesTargetRepository.findByOrganizationIdAndEmployeeId(orgId, empId))
            .thenReturn(listOf(target))

        `when`(
            invoiceRepository.sumAmountBySalespersonAndPeriod(
                orgId,
                empId,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
            ),
        ).thenReturn(BigDecimal("75000.00"))

        val report = service.getAttainmentReportForEmployee(orgId, empId)

        assertEquals(1, report.size)
        assertTrue(BigDecimal("75000.00").compareTo(report[0].actualAmount) == 0)
        assertTrue(BigDecimal("75.00").compareTo(report[0].attainmentPercentage) == 0)
    }
}
