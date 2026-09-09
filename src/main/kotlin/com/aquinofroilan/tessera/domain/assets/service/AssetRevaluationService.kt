package com.aquinofroilan.tessera.domain.assets.service

import com.aquinofroilan.tessera.domain.assets.dto.AssetRevaluationRequest
import com.aquinofroilan.tessera.domain.assets.dto.AssetRevaluationResponse
import com.aquinofroilan.tessera.domain.assets.model.AssetRevaluation
import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.repository.AssetRevaluationRepository
import com.aquinofroilan.tessera.domain.assets.repository.FixedAssetRepository
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryLine
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class AssetRevaluationService(
    private val assetRevaluationRepository: AssetRevaluationRepository,
    private val fixedAssetRepository: FixedAssetRepository,
    private val fixedAssetService: FixedAssetService,
    private val journalEntryService: JournalEntryService,
    private val accountRepository: AccountRepository,
) {
    private val log = LoggerFactory.getLogger(AssetRevaluationService::class.java)

    @Transactional
    fun revalueAsset(
        organizationId: UUID,
        assetId: UUID,
        request: AssetRevaluationRequest,
        userId: UUID,
    ): AssetRevaluationResponse {
        val asset = fixedAssetService.getAsset(assetId, organizationId)
        if (asset.status != AssetStatus.ACTIVE) {
            throw BusinessRuleException("Only active assets can be revalued")
        }

        val previousCost = asset.acquisitionCost
        val newCost = request.newCost!!
        val revaluationAmount = newCost.subtract(previousCost)

        if (revaluationAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw BusinessRuleException("New cost is the same as the previous cost")
        }

        val assetAccountId = asset.assetAccountId?.let { UUID.fromString(it) }
            ?: throw BusinessRuleException("Asset does not have an asset account configured")
        
        val revaluationAccountId = request.revaluationAccountId!!

        val accounts = accountRepository.findAllById(listOf(assetAccountId, revaluationAccountId)).associateBy { it.id }
        val assetAccount = accounts[assetAccountId] ?: throw BusinessRuleException("Asset account not found")
        val revalAccount = accounts[revaluationAccountId] ?: throw BusinessRuleException("Revaluation account not found")

        val lines = mutableListOf<JournalEntryLine>()

        if (revaluationAmount > BigDecimal.ZERO) {
            // Revaluation gain (Dr Asset, Cr Revaluation Reserve)
            lines.add(
                JournalEntryLine(
                    accountId = assetAccount.id,
                    accountCode = assetAccount.code,
                    accountName = assetAccount.name,
                    debit = revaluationAmount,
                    credit = BigDecimal.ZERO,
                )
            )
            lines.add(
                JournalEntryLine(
                    accountId = revalAccount.id,
                    accountCode = revalAccount.code,
                    accountName = revalAccount.name,
                    debit = BigDecimal.ZERO,
                    credit = revaluationAmount,
                )
            )
        } else {
            // Revaluation loss / impairment (Dr Revaluation Reserve / Impairment Expense, Cr Asset)
            val lossAmount = revaluationAmount.abs()
            lines.add(
                JournalEntryLine(
                    accountId = revalAccount.id,
                    accountCode = revalAccount.code,
                    accountName = revalAccount.name,
                    debit = lossAmount,
                    credit = BigDecimal.ZERO,
                )
            )
            lines.add(
                JournalEntryLine(
                    accountId = assetAccount.id,
                    accountCode = assetAccount.code,
                    accountName = assetAccount.name,
                    debit = BigDecimal.ZERO,
                    credit = lossAmount,
                )
            )
        }

        var revaluation = AssetRevaluation(
            organizationId = organizationId,
            assetId = assetId,
            revaluationDate = request.revaluationDate!!,
            previousCost = previousCost,
            newCost = newCost,
            revaluationAmount = revaluationAmount,
            revaluationAccountId = revaluationAccountId,
            reason = request.reason,
            createdBy = userId,
        )

        revaluation = assetRevaluationRepository.save(revaluation)

        val journalEntry = journalEntryService.createSystemEntry(
            date = request.revaluationDate,
            description = "Revaluation of asset ${asset.assetNumber}",
            organizationId = organizationId,
            lines = lines,
            sourceReference = "asset_revaluation:${revaluation.id}",
            createdBy = userId,
        )

        revaluation.journalEntryId = journalEntry.id
        assetRevaluationRepository.save(revaluation)

        // Update the asset's acquisition cost
        asset.acquisitionCost = newCost
        fixedAssetRepository.save(asset)

        log.info("Asset {} revalued by amount {}", asset.assetNumber, revaluationAmount)

        return mapToResponse(revaluation)
    }

    fun getAssetRevaluations(
        organizationId: UUID,
        assetId: UUID,
    ): List<AssetRevaluationResponse> {
        return assetRevaluationRepository
            .findByOrganizationIdAndAssetId(organizationId, assetId)
            .sortedByDescending { it.revaluationDate }
            .map { mapToResponse(it) }
    }

    private fun mapToResponse(reval: AssetRevaluation): AssetRevaluationResponse {
        return AssetRevaluationResponse(
            id = reval.id,
            assetId = reval.assetId,
            revaluationDate = reval.revaluationDate.toString(),
            previousCost = reval.previousCost,
            newCost = reval.newCost,
            revaluationAmount = reval.revaluationAmount,
            revaluationAccountId = reval.revaluationAccountId,
            journalEntryId = reval.journalEntryId,
            reason = reval.reason,
            createdBy = reval.createdBy,
            createdAt = reval.createdAt?.toString() ?: "",
        )
    }
}
