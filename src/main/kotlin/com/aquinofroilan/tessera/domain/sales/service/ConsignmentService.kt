package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.service.InvoiceService
import com.aquinofroilan.tessera.domain.inventory.service.WarehouseService
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesOrderLineRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.domain.sales.dto.GenerateInvoiceRequest
import com.aquinofroilan.tessera.domain.sales.model.SalesOrder
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class ConsignmentService(
    private val salesOrderService: SalesOrderService,
    private val invoiceService: InvoiceService,
    private val warehouseService: WarehouseService,
    private val customerService: CustomerService,
) {
    @Transactional
    fun recognizeUsage(
        organizationId: UUID,
        customerId: UUID,
        warehouseId: UUID,
        productId: UUID,
        quantity: BigDecimal,
        unitPrice: BigDecimal,
        userId: UUID,
    ): SalesOrder {
        val warehouse = warehouseService.getWarehouse(warehouseId, organizationId)
        if (!warehouse.isConsignment) {
            throw BusinessRuleException("Warehouse is not a consignment location")
        }
        if (warehouse.customerId != customerId) {
            throw BusinessRuleException("Consignment warehouse does not belong to the specified customer")
        }

        val createSoRequest =
            CreateSalesOrderRequest(
                customerId = customerId,
                warehouseId = warehouseId,
                orderDate = LocalDate.now(),
                expectedDate = LocalDate.now(),
                referenceNumber = "Consignment Usage",
                lines =
                    listOf(
                        CreateSalesOrderLineRequest(
                            productId = productId,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            description = "Consignment Stock Recognition",
                        ),
                    ),
            )

        val so = salesOrderService.createSalesOrder(createSoRequest, organizationId, userId)
        val approvedSo = salesOrderService.approveSalesOrder(so.id, organizationId, userId)
        val fulfilledSo = salesOrderService.fulfillSalesOrder(approvedSo.id, null, organizationId, userId)

        val invoiceRequest = GenerateInvoiceRequest()
        val invoice = salesOrderService.generateInvoice(fulfilledSo.id, invoiceRequest, organizationId, userId)

        invoiceService.approveInvoice(invoice.id, organizationId, userId)

        return fulfilledSo
    }
}
