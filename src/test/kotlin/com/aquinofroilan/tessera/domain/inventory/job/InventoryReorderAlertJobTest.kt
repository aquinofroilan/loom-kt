package com.aquinofroilan.tessera.domain.inventory.job

import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.inventory.model.InventoryReorderRule
import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.model.StockOnHand
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryReorderRuleRepository
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandRepository
import com.aquinofroilan.tessera.domain.notification.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.domain.notification.model.NotificationCategory
import com.aquinofroilan.tessera.domain.notification.service.NotificationService
import com.aquinofroilan.tessera.domain.procurement.repository.PurchaseOrderRepository
import org.jobrunr.scheduling.JobScheduler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class InventoryReorderAlertJobTest {
    @Mock
    private lateinit var ruleRepository: InventoryReorderRuleRepository

    @Mock
    private lateinit var stockRepository: StockOnHandRepository

    @Mock
    private lateinit var poRepository: PurchaseOrderRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var notificationService: NotificationService

    @Mock
    private lateinit var jobScheduler: JobScheduler

    @InjectMocks
    private lateinit var job: InventoryReorderAlertJob

    @Test
    fun `should send notification when stock is below reorder point and no incoming POs`() {
        val orgId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val warehouseId = UUID.randomUUID()
        val buyerId = UUID.randomUUID()

        val rule =
            InventoryReorderRule(
                organizationId = orgId,
                productId = productId,
                warehouseId = warehouseId,
                reorderPoint = BigDecimal("100"),
                safetyStock = BigDecimal("20"),
            )

        val stock =
            StockOnHand(
                organizationId = orgId,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("50"), // Below 100
            )

        val product =
            Product(
                id = productId,
                organizationId = orgId,
                sku = "SKU-123",
                name = "Test Product",
                listPrice = BigDecimal("10.00"),
                priceCurrency = "USD",
            )

        val buyer =
            User(
                uuid = buyerId,
                organizationId = orgId,
                username = "buyer",
                email = "buyer@test.com",
                firstName = "Buyer",
                lastName = "Test",
                passwordHash = "hash",
            )

        `when`(ruleRepository.findAll()).thenReturn(listOf(rule))
        `when`(
            userRepository.findByOrganizationIdAndRolesIn(orgId, listOf("PROCUREMENT_MANAGER", "BUYER", "ADMIN")),
        ).thenReturn(listOf(buyer))
        `when`(stockRepository.findByOrganizationIdAndProductIdAndWarehouseId(orgId, productId, warehouseId)).thenReturn(Optional.of(stock))
        `when`(poRepository.getPendingIncomingQuantity(orgId, productId, warehouseId)).thenReturn(BigDecimal.ZERO)
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(product))

        job.execute()

        val requestCaptor = argumentCaptor<CreateNotificationRequest>()
        verify(notificationService).publish(requestCaptor.capture(), org.mockito.kotlin.eq(orgId))

        val req = requestCaptor.firstValue
        assertEquals(buyerId, req.recipientUserId)
        assertEquals(NotificationCategory.SYSTEM, req.category)
        assertEquals("REORDER_ALERT", req.kind)
    }

    @Test
    fun `should not send notification when stock + pending incoming is above reorder point`() {
        val orgId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val warehouseId = UUID.randomUUID()

        val rule =
            InventoryReorderRule(
                organizationId = orgId,
                productId = productId,
                warehouseId = warehouseId,
                reorderPoint = BigDecimal("100"),
            )

        val stock =
            StockOnHand(
                organizationId = orgId,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("50"), // Below 100
            )

        val buyer =
            User(
                uuid = UUID.randomUUID(),
                organizationId = orgId,
                username = "buyer",
                email = "buyer@test.com",
                firstName = "Buyer",
                lastName = "Test",
                passwordHash = "hash",
            )

        `when`(ruleRepository.findAll()).thenReturn(listOf(rule))
        `when`(
            userRepository.findByOrganizationIdAndRolesIn(orgId, listOf("PROCUREMENT_MANAGER", "BUYER", "ADMIN")),
        ).thenReturn(listOf(buyer))
        `when`(stockRepository.findByOrganizationIdAndProductIdAndWarehouseId(orgId, productId, warehouseId)).thenReturn(Optional.of(stock))
        // 60 pending + 50 current = 110 > 100
        `when`(poRepository.getPendingIncomingQuantity(orgId, productId, warehouseId)).thenReturn(BigDecimal("60"))

        job.execute()

        verifyNoInteractions(notificationService)
    }
}
