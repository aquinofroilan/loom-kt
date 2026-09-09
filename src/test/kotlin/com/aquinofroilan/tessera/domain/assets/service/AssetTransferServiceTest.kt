package com.aquinofroilan.tessera.domain.assets.service

import com.aquinofroilan.tessera.domain.assets.dto.AssetTransferRequest
import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.model.AssetTransfer
import com.aquinofroilan.tessera.domain.assets.model.FixedAsset
import com.aquinofroilan.tessera.domain.assets.repository.AssetTransferRepository
import com.aquinofroilan.tessera.domain.assets.repository.FixedAssetRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class AssetTransferServiceTest {

    private val assetTransferRepository: AssetTransferRepository = mock()
    private val fixedAssetRepository: FixedAssetRepository = mock()
    private val fixedAssetService: FixedAssetService = mock()

    private lateinit var assetTransferService: AssetTransferService

    private val orgId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val assetId = UUID.randomUUID()

    private lateinit var asset: FixedAsset

    @BeforeEach
    fun setup() {
        assetTransferService = AssetTransferService(
            assetTransferRepository,
            fixedAssetRepository,
            fixedAssetService
        )

        asset = FixedAsset(
            id = assetId,
            organizationId = orgId,
            assetNumber = "FA-001",
            name = "Laptop",
            acquisitionDate = LocalDate.now().minusYears(1),
            acquisitionCost = BigDecimal("1000.00"),
            usefulLifeMonths = 36,
            location = "HQ",
            status = AssetStatus.ACTIVE
        )
    }

    @Test
    fun `transferAsset updates asset location and saves transfer record`() {
        whenever(fixedAssetService.getAsset(assetId, orgId)).thenReturn(asset)
        whenever(assetTransferRepository.save(any<AssetTransfer>())).thenAnswer { it.arguments[0] as AssetTransfer }

        val request = AssetTransferRequest(
            transferDate = LocalDate.now(),
            toLocation = "Branch A",
            reason = "Employee relocation"
        )

        val response = assetTransferService.transferAsset(orgId, assetId, request, userId)

        assertEquals("Branch A", response.toLocation)
        assertEquals("HQ", response.fromLocation)
        assertEquals(assetId, response.assetId)

        verify(fixedAssetRepository).save(asset)
        assertEquals("Branch A", asset.location)
    }

    @Test
    fun `transferAsset throws exception if asset is not active`() {
        asset.status = AssetStatus.DISPOSED
        whenever(fixedAssetService.getAsset(assetId, orgId)).thenReturn(asset)

        val request = AssetTransferRequest(
            transferDate = LocalDate.now(),
            toLocation = "Branch A",
            reason = "Employee relocation"
        )

        assertThrows(BusinessRuleException::class.java) {
            assetTransferService.transferAsset(orgId, assetId, request, userId)
        }
    }
}
