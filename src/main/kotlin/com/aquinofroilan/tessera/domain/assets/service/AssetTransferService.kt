package com.aquinofroilan.tessera.domain.assets.service

import com.aquinofroilan.tessera.domain.assets.dto.AssetTransferRequest
import com.aquinofroilan.tessera.domain.assets.dto.AssetTransferResponse
import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.model.AssetTransfer
import com.aquinofroilan.tessera.domain.assets.repository.AssetTransferRepository
import com.aquinofroilan.tessera.domain.assets.repository.FixedAssetRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AssetTransferService(
    private val assetTransferRepository: AssetTransferRepository,
    private val fixedAssetRepository: FixedAssetRepository,
    private val fixedAssetService: FixedAssetService,
) {
    private val log = LoggerFactory.getLogger(AssetTransferService::class.java)

    @Transactional
    fun transferAsset(
        organizationId: UUID,
        assetId: UUID,
        request: AssetTransferRequest,
        userId: UUID,
    ): AssetTransferResponse {
        val asset = fixedAssetService.getAsset(assetId, organizationId)
        if (asset.status != AssetStatus.ACTIVE) {
            throw BusinessRuleException("Only active assets can be transferred")
        }

        val transfer =
            AssetTransfer(
                organizationId = organizationId,
                assetId = assetId,
                transferDate = request.transferDate!!,
                fromLocation = asset.location,
                toLocation = request.toLocation!!,
                reason = request.reason,
                createdBy = userId,
            )

        // Update the asset's location
        asset.location = request.toLocation
        fixedAssetRepository.save(asset)

        val savedTransfer = assetTransferRepository.save(transfer)
        log.info("Asset {} transferred to location {}", asset.assetNumber, request.toLocation)

        return mapToResponse(savedTransfer)
    }

    fun getAssetTransfers(
        organizationId: UUID,
        assetId: UUID,
    ): List<AssetTransferResponse> =
        assetTransferRepository
            .findByOrganizationIdAndAssetId(organizationId, assetId)
            .sortedByDescending { it.transferDate }
            .map { mapToResponse(it) }

    private fun mapToResponse(transfer: AssetTransfer): AssetTransferResponse =
        AssetTransferResponse(
            id = transfer.id,
            assetId = transfer.assetId,
            transferDate = transfer.transferDate.toString(),
            fromLocation = transfer.fromLocation,
            toLocation = transfer.toLocation,
            reason = transfer.reason,
            createdBy = transfer.createdBy,
            createdAt = transfer.createdAt?.toString() ?: "",
        )
}
