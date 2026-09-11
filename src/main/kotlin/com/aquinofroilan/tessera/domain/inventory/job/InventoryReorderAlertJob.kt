package com.aquinofroilan.tessera.domain.inventory.job

import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryReorderRuleRepository
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandRepository
import com.aquinofroilan.tessera.domain.notification.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.domain.notification.model.NotificationCategory
import com.aquinofroilan.tessera.domain.notification.service.NotificationService
import com.aquinofroilan.tessera.domain.procurement.repository.PurchaseOrderRepository
import jakarta.annotation.PostConstruct
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.scheduling.JobScheduler
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class InventoryReorderAlertJob(
    private val ruleRepository: InventoryReorderRuleRepository,
    private val stockRepository: StockOnHandRepository,
    private val poRepository: PurchaseOrderRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val jobScheduler: JobScheduler,
) {
    @Value("\${tessera.inventory.reorder-alert.cron:0 0 * * *}")
    private lateinit var cronExpression: String

    @PostConstruct
    fun scheduleJob() {
        jobScheduler.scheduleRecurrently("inventory-reorder-alert-job", cronExpression) {
            execute()
        }
    }

    @Job(name = "Inventory Reorder Alert Job")
    @Transactional
    fun execute() {
        val rules = ruleRepository.findAll()
        val rulesByOrg = rules.groupBy { it.organizationId }

        for ((orgId, orgRules) in rulesByOrg) {
            val buyers = userRepository.findByOrganizationIdAndRolesIn(orgId, listOf("PROCUREMENT_MANAGER", "BUYER", "ADMIN"))

            for (rule in orgRules) {
                val stock =
                    stockRepository
                        .findByOrganizationIdAndProductIdAndWarehouseId(
                            rule.organizationId,
                            rule.productId,
                            rule.warehouseId,
                        ).orElse(null)

                val currentQuantity = stock?.quantity ?: BigDecimal.ZERO

                if (currentQuantity <= rule.reorderPoint) {
                    val pendingIncoming =
                        poRepository.getPendingIncomingQuantity(
                            rule.organizationId,
                            rule.productId,
                            rule.warehouseId,
                        ) ?: BigDecimal.ZERO

                    val totalAvailable = currentQuantity + pendingIncoming

                    if (totalAvailable <= rule.reorderPoint) {
                        val product = productRepository.findById(rule.productId).orElse(null)
                        val sku = product?.sku ?: "Unknown"
                        val productName = product?.name ?: "Unknown Product"

                        // Send notification to all buyers
                        for (buyer in buyers) {
                            notificationService.publish(
                                CreateNotificationRequest(
                                    recipientUserId = buyer.uuid,
                                    category = NotificationCategory.SYSTEM,
                                    kind = "REORDER_ALERT",
                                    title = "Reorder Alert: \$productName",
                                    body =
                                        "Stock for \$sku is currently \$currentQuantity, which is below the reorder point " +
                                            "of \${rule.reorderPoint}. Pending incoming: \$pendingIncoming.",
                                    link = "/inventory/products/\${rule.productId}",
                                ),
                                organizationId = rule.organizationId,
                            )
                        }
                    }
                }
            }
        }
    }
}
