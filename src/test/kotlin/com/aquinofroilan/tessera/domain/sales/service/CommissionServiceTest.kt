package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.event.InvoiceApprovedEvent
import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.sales.model.CommissionAccrual
import com.aquinofroilan.tessera.domain.sales.model.CommissionAssignment
import com.aquinofroilan.tessera.domain.sales.model.CommissionPlan
import com.aquinofroilan.tessera.domain.sales.model.CommissionPlanType
import com.aquinofroilan.tessera.domain.sales.model.CommissionStatement
import com.aquinofroilan.tessera.domain.sales.model.CommissionStatementStatus
import com.aquinofroilan.tessera.domain.sales.repository.CommissionAccrualRepository
import com.aquinofroilan.tessera.domain.sales.repository.CommissionAssignmentRepository
import com.aquinofroilan.tessera.domain.sales.repository.CommissionPlanRepository
import com.aquinofroilan.tessera.domain.sales.repository.CommissionStatementRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CommissionServiceTest {
    @Mock
    private lateinit var planRepository: CommissionPlanRepository

    @Mock
    private lateinit var assignmentRepository: CommissionAssignmentRepository

    @Mock
    private lateinit var accrualRepository: CommissionAccrualRepository

    @Mock
    private lateinit var statementRepository: CommissionStatementRepository

    @InjectMocks
    private lateinit var service: CommissionService

    @Test
    fun `should accrue commission from invoice approval`() {
        val orgId = UUID.randomUUID()
        val empId = UUID.randomUUID()
        val invId = UUID.randomUUID()
        val planId = UUID.randomUUID()

        val invoice =
            Invoice(
                id = invId,
                invoiceNumber = "INV-001",
                customerId = UUID.randomUUID(),
                customerName = "Test",
                date = LocalDate.now(),
                dueDate = LocalDate.now(),
                organizationId = orgId,
                lines = emptyList(),
                totalAmount = BigDecimal("1000.00"),
                salespersonId = empId,
                createdBy = UUID.randomUUID(),
            )

        val plan =
            CommissionPlan(
                id = planId,
                name = "Flat 5%",
                type = CommissionPlanType.FLAT_RATE,
                flatRatePercentage = BigDecimal("5.00"),
                organizationId = orgId,
            )

        val assignment =
            CommissionAssignment(
                employeeId = empId,
                planId = planId,
                organizationId = orgId,
            )

        `when`(accrualRepository.existsByInvoiceIdAndEmployeeId(invId, empId)).thenReturn(false)
        `when`(assignmentRepository.findByOrganizationIdAndEmployeeId(orgId, empId)).thenReturn(listOf(assignment))
        `when`(planRepository.findById(planId)).thenReturn(Optional.of(plan))

        service.onInvoiceApproved(InvoiceApprovedEvent(invoice))

        val captor = argumentCaptor<CommissionAccrual>()
        verify(accrualRepository).save(captor.capture())

        val accrual = captor.firstValue
        assertEquals(empId, accrual.employeeId)
        assertEquals(invId, accrual.invoiceId)
        assertTrue(BigDecimal("50.00").compareTo(accrual.amount) == 0) // 1000 * 5%
    }

    @Test
    fun `should generate commission statement`() {
        val orgId = UUID.randomUUID()
        val empId = UUID.randomUUID()

        val accrual1 =
            CommissionAccrual(
                employeeId = empId,
                invoiceId = UUID.randomUUID(),
                amount = BigDecimal("50.00"),
                currency = "USD",
                organizationId = orgId,
            )

        val accrual2 =
            CommissionAccrual(
                employeeId = empId,
                invoiceId = UUID.randomUUID(),
                amount = BigDecimal("150.00"),
                currency = "USD",
                organizationId = orgId,
            )

        val savedStatement =
            CommissionStatement(
                id = UUID.randomUUID(),
                employeeId = empId,
                periodStart = LocalDate.now(),
                periodEnd = LocalDate.now(),
                totalAmount = BigDecimal("200.00"),
                organizationId = orgId,
                status = CommissionStatementStatus.DRAFT,
            )

        `when`(accrualRepository.findByOrganizationIdAndEmployeeIdAndStatementIdIsNull(orgId, empId))
            .thenReturn(listOf(accrual1, accrual2))

        `when`(statementRepository.save(any())).thenReturn(savedStatement)

        val statement = service.generateStatement(orgId, empId, LocalDate.now(), LocalDate.now())

        assertEquals(savedStatement.id, statement.id)

        verify(accrualRepository).save(accrual1)
        verify(accrualRepository).save(accrual2)
        assertEquals(statement.id, accrual1.statementId)
        assertEquals(statement.id, accrual2.statementId)
    }

    private fun assertTrue(b: Boolean) {
        if (!b) throw AssertionError("Expected true")
    }
}
