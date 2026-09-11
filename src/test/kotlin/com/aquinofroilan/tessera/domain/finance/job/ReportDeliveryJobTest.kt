package com.aquinofroilan.tessera.domain.finance.job

import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandRepository
import com.aquinofroilan.tessera.domain.notification.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.domain.notification.service.NotificationService
import com.aquinofroilan.tessera.domain.organization.model.Organizations
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.platform.model.Attachment
import com.aquinofroilan.tessera.domain.platform.service.AttachmentService
import com.aquinofroilan.tessera.domain.procurement.repository.PurchaseOrderRepository
import com.aquinofroilan.tessera.domain.sales.repository.SalesOrderRepository
import org.jobrunr.scheduling.JobScheduler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ReportDeliveryJobTest {
    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var salesOrderRepository: SalesOrderRepository

    @Mock
    private lateinit var purchaseOrderRepository: PurchaseOrderRepository

    @Mock
    private lateinit var stockOnHandRepository: StockOnHandRepository

    @Mock
    private lateinit var notificationService: NotificationService

    @Mock
    private lateinit var attachmentService: AttachmentService

    @Mock
    private lateinit var jobScheduler: JobScheduler

    @InjectMocks
    private lateinit var job: ReportDeliveryJob

    @Test
    fun `should generate and deliver weekly executive report`() {
        val orgId = UUID.randomUUID()
        val execId = UUID.randomUUID()
        val attachmentId = UUID.randomUUID()

        val org =
            Organizations(
                uuid = orgId,
                orgSlug = "test-org",
                name = "Test Org",
                legalName = "Test Org Inc",
                tradeName = "Test Org",
                baseCurrency = "USD",
                isActive = true,
                fiscalYearStart = java.time.LocalDateTime.now(),
                timezone = "UTC",
            )
        val exec =
            User(
                uuid = execId,
                organizationId = orgId,
                username = "exec",
                email = "exec@test.com",
                firstName = "Exec",
                lastName = "Test",
                passwordHash = "hash",
            )

        val attachment =
            Attachment(
                id = attachmentId,
                organizationId = orgId,
                entityType = "WeeklyReport",
                entityId = orgId,
                filename = "report.csv",
                mimeType = "text/csv",
                sizeBytes = 100L,
                storageKey = "path/to/report.csv",
                uploadedBy = execId,
            )

        `when`(organizationRepository.findAll()).thenReturn(listOf(org))
        `when`(userRepository.findByOrganizationIdAndRolesIn(orgId, listOf("OWNER", "SUPER_ADMIN"))).thenReturn(listOf(exec))
        `when`(salesOrderRepository.sumSalesVolumeSince(eq(orgId), any())).thenReturn(BigDecimal("5000.00"))
        `when`(purchaseOrderRepository.sumPurchaseVolumeSince(eq(orgId), any())).thenReturn(BigDecimal("2000.00"))
        `when`(stockOnHandRepository.calculateInventoryValuation(orgId)).thenReturn(BigDecimal("15000.00"))

        `when`(
            attachmentService.uploadBytes(
                any(),
                any(),
                eq("text/csv"),
                eq("WeeklyReport"),
                eq(orgId),
                eq(orgId),
                eq(execId),
            ),
        ).thenReturn(attachment)

        job.execute()

        val requestCaptor = argumentCaptor<CreateNotificationRequest>()
        verify(notificationService).publish(requestCaptor.capture(), eq(orgId))

        val req = requestCaptor.firstValue
        println("BODY: ${req.body}")
        assertEquals(execId, req.recipientUserId)
        assertEquals("EXECUTIVE_REPORT", req.kind)
        assertEquals("Your weekly summary report is ready. Sales: \$5000.00, AP: \$2000.00, Inventory: \$15000.00.", req.body)
        assertTrue(req.body!!.contains("2000.00"))
        assertEquals("/api/v1/attachments/$attachmentId/download", req.link)
    }
}
