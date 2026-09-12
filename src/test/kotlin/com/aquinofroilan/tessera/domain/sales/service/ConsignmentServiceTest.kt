package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.service.InvoiceService
import com.aquinofroilan.tessera.domain.inventory.model.Warehouse
import com.aquinofroilan.tessera.domain.inventory.service.WarehouseService
import com.aquinofroilan.tessera.domain.sales.model.SalesOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ConsignmentServiceTest {
    @Mock
    private lateinit var salesOrderService: SalesOrderService

    @Mock
    private lateinit var invoiceService: InvoiceService

    @Mock
    private lateinit var warehouseService: WarehouseService

    @Mock
    private lateinit var customerService: CustomerService

    @InjectMocks
    private lateinit var service: ConsignmentService

    @Test
    fun `should recognize usage by creating SO and Invoice`() {
        val orgId = UUID.randomUUID()
        val custId = UUID.randomUUID()
        val warehouseId = UUID.randomUUID()
        val prodId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val warehouse =
            Warehouse(
                id = warehouseId,
                code = "CONS",
                name = "Consignment",
                isConsignment = true,
                customerId = custId,
                organizationId = orgId,
            )

        val so =
            SalesOrder(
                id = UUID.randomUUID(),
                organizationId = orgId,
                soNumber = "SO-123",
                customerId = custId,
                customerName = "Customer",
                warehouseId = warehouseId,
                orderDate = LocalDate.now(),
                lines = emptyList(),
                totalAmount = BigDecimal("100.00"),
                createdBy = userId,
            )

        val invoice =
            Invoice(
                id = UUID.randomUUID(),
                invoiceNumber = "INV-123",
                customerId = custId,
                customerName = "Customer",
                date = LocalDate.now(),
                dueDate = LocalDate.now(),
                organizationId = orgId,
                lines = emptyList(),
                totalAmount = BigDecimal("100.00"),
                createdBy = userId,
            )

        `when`(warehouseService.getWarehouse(warehouseId, orgId)).thenReturn(warehouse)
        `when`(salesOrderService.createSalesOrder(any(), eq(orgId), eq(userId))).thenReturn(so)
        `when`(salesOrderService.approveSalesOrder(so.id, orgId, userId)).thenReturn(so)
        `when`(salesOrderService.fulfillSalesOrder(eq(so.id), eq(null), eq(orgId), eq(userId))).thenReturn(so)
        `when`(salesOrderService.generateInvoice(eq(so.id), any(), eq(orgId), eq(userId))).thenReturn(invoice)

        service.recognizeUsage(orgId, custId, warehouseId, prodId, BigDecimal("10.00"), BigDecimal("10.00"), userId)

        verify(invoiceService).approveInvoice(invoice.id, orgId, userId)
    }
}
