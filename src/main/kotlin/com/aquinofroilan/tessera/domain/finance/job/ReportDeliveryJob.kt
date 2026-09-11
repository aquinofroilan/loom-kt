package com.aquinofroilan.tessera.domain.finance.job

import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandRepository
import com.aquinofroilan.tessera.domain.notification.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.domain.notification.model.NotificationCategory
import com.aquinofroilan.tessera.domain.notification.service.NotificationService
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.platform.service.AttachmentService
import com.aquinofroilan.tessera.domain.procurement.repository.PurchaseOrderRepository
import com.aquinofroilan.tessera.domain.sales.repository.SalesOrderRepository
import jakarta.annotation.PostConstruct
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.scheduling.JobScheduler
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class ReportDeliveryJob(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val stockOnHandRepository: StockOnHandRepository,
    private val notificationService: NotificationService,
    private val attachmentService: AttachmentService,
    private val jobScheduler: JobScheduler,
) {
    @Value("\${tessera.finance.report-delivery.cron:0 0 * * MON}")
    private lateinit var cronExpression: String

    @PostConstruct
    fun scheduleJob() {
        jobScheduler.scheduleRecurrently("weekly-executive-report", cronExpression) {
            execute()
        }
    }

    @Job(name = "Weekly Executive Report Delivery Job")
    @Transactional
    fun execute() {
        val orgs = organizationRepository.findAll().filter { it.isActive }
        val startDate = LocalDate.now().minusDays(7)

        for (org in orgs) {
            val execs = userRepository.findByOrganizationIdAndRolesIn(org.uuid, listOf("OWNER", "SUPER_ADMIN"))
            if (execs.isEmpty()) continue

            val salesVolume = salesOrderRepository.sumSalesVolumeSince(org.uuid, startDate)
            val apVolume = purchaseOrderRepository.sumPurchaseVolumeSince(org.uuid, startDate)
            val inventoryValuation = stockOnHandRepository.calculateInventoryValuation(org.uuid)

            val csvContent =
                buildString {
                    appendLine("Metric,Value")
                    appendLine("Sales Volume (Trailing Week),\$salesVolume")
                    appendLine("Accounts Payable Volume (Trailing Week),\$apVolume")
                    appendLine("Inventory Valuation,\$inventoryValuation")
                }

            val filename = "Executive_Report_${LocalDate.now()}.csv"

            // Just use the first executive's ID as the uploader for the attachment
            val uploaderId = execs.first().uuid

            val attachment =
                attachmentService.uploadBytes(
                    bytes = csvContent.toByteArray(),
                    filename = filename,
                    mimeType = "text/csv",
                    entityType = "WeeklyReport",
                    entityId = org.uuid,
                    organizationId = org.uuid,
                    userId = uploaderId,
                )

            val downloadLink = "/api/v1/attachments/${attachment.id}/download"

            for (exec in execs) {
                notificationService.publish(
                    CreateNotificationRequest(
                        recipientUserId = exec.uuid,
                        category = NotificationCategory.INFO,
                        kind = "EXECUTIVE_REPORT",
                        title = "Weekly Executive Report",
                        body =
                            "Your weekly summary report is ready. Sales: \$$salesVolume, " +
                                "AP: \$$apVolume, Inventory: \$$inventoryValuation.",
                        link = downloadLink,
                    ),
                    organizationId = org.uuid,
                )
            }
        }
    }
}
