package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.event.InvoiceApprovedEvent
import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.sales.model.CreditNote
import com.aquinofroilan.tessera.domain.sales.model.RebateAccrual
import com.aquinofroilan.tessera.domain.sales.model.RebateProgram
import com.aquinofroilan.tessera.domain.sales.model.RebateType
import com.aquinofroilan.tessera.domain.sales.repository.CreditNoteRepository
import com.aquinofroilan.tessera.domain.sales.repository.RebateAccrualRepository
import com.aquinofroilan.tessera.domain.sales.repository.RebateProgramRepository
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
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class RebateServiceTest {
    @Mock
    private lateinit var rebateProgramRepository: RebateProgramRepository

    @Mock
    private lateinit var rebateAccrualRepository: RebateAccrualRepository

    @Mock
    private lateinit var creditNoteRepository: CreditNoteRepository

    @InjectMocks
    private lateinit var service: RebateService

    @Test
    fun `should accrue rebate on invoice approval`() {
        val orgId = UUID.randomUUID()
        val custId = UUID.randomUUID()
        val invId = UUID.randomUUID()

        val program =
            RebateProgram(
                id = UUID.randomUUID(),
                name = "Volume Rebate 5%",
                type = RebateType.VOLUME_PERCENTAGE,
                customerId = custId,
                rateValue = BigDecimal("5.0"),
                periodStart = LocalDate.now().minusDays(1),
                periodEnd = LocalDate.now().plusDays(10),
                organizationId = orgId,
            )

        val invoice =
            Invoice(
                id = invId,
                invoiceNumber = "INV-123",
                customerId = custId,
                customerName = "Customer",
                date = LocalDate.now(),
                dueDate = LocalDate.now(),
                totalAmount = BigDecimal("1000.00"),
                currencyCode = "USD",
                organizationId = orgId,
                lines = emptyList(),
                createdBy = UUID.randomUUID(),
            )

        `when`(rebateProgramRepository.findByOrganizationIdAndCustomerId(orgId, custId))
            .thenReturn(listOf(program))
        `when`(rebateAccrualRepository.existsByInvoiceIdAndRebateProgramId(invId, program.id))
            .thenReturn(false)

        service.onInvoiceApproved(InvoiceApprovedEvent(invoice))

        val captor = argumentCaptor<RebateAccrual>()
        verify(rebateAccrualRepository).save(captor.capture())

        val accrual = captor.firstValue
        assertEquals(program.id, accrual.rebateProgramId)
        assertEquals(invId, accrual.invoiceId)
        assertTrue(BigDecimal("50.00").compareTo(accrual.amount) == 0) // 1000 * 5%
    }

    @Test
    fun `should settle rebates and generate credit note`() {
        val orgId = UUID.randomUUID()
        val custId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val accrual =
            RebateAccrual(
                rebateProgramId = UUID.randomUUID(),
                customerId = custId,
                invoiceId = UUID.randomUUID(),
                amount = BigDecimal("150.00"),
                currency = "USD",
                organizationId = orgId,
            )

        `when`(rebateAccrualRepository.findByOrganizationIdAndCustomerIdAndIsSettledFalse(orgId, custId))
            .thenReturn(listOf(accrual))

        `when`(creditNoteRepository.save(any())).thenAnswer { it.getArgument(0) as CreditNote }

        val cn = service.settleRebates(orgId, custId, userId)

        assertTrue(cn != null)
        assertTrue(BigDecimal("150.00").compareTo(cn.totalAmount) == 0)
        assertEquals(custId, cn.customerId)
        assertTrue(accrual.isSettled)
        verify(rebateAccrualRepository).save(accrual)
    }
}
