package com.aquinofroilan.tessera.domain.assets.service

import com.aquinofroilan.tessera.domain.assets.dto.AssetRevaluationRequest
import com.aquinofroilan.tessera.domain.assets.model.AssetRevaluation
import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.model.FixedAsset
import com.aquinofroilan.tessera.domain.assets.repository.AssetRevaluationRepository
import com.aquinofroilan.tessera.domain.assets.repository.FixedAssetRepository
import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.AccountType
import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntrySource
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryStatus
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class AssetRevaluationServiceTest {
    private val assetRevaluationRepository: AssetRevaluationRepository = mock()
    private val fixedAssetRepository: FixedAssetRepository = mock()
    private val fixedAssetService: FixedAssetService = mock()
    private val journalEntryService: JournalEntryService = mock()
    private val accountRepository: AccountRepository = mock()

    private lateinit var assetRevaluationService: AssetRevaluationService

    private val orgId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val assetId = UUID.randomUUID()
    private val assetAccountId = UUID.randomUUID()
    private val revalAccountId = UUID.randomUUID()

    private lateinit var asset: FixedAsset
    private lateinit var assetAccount: Account
    private lateinit var revalAccount: Account

    @BeforeEach
    fun setup() {
        assetRevaluationService =
            AssetRevaluationService(
                assetRevaluationRepository,
                fixedAssetRepository,
                fixedAssetService,
                journalEntryService,
                accountRepository,
            )

        asset =
            FixedAsset(
                id = assetId,
                organizationId = orgId,
                assetNumber = "FA-001",
                name = "Building",
                acquisitionDate = LocalDate.now().minusYears(5),
                acquisitionCost = BigDecimal("100000.00"),
                usefulLifeMonths = 360,
                assetAccountId = assetAccountId.toString(),
                status = AssetStatus.ACTIVE,
            )

        assetAccount =
            Account(
                id = assetAccountId,
                organizationId = orgId,
                code = "1500",
                name = "Buildings",
                type = AccountType.ASSET,
            )

        revalAccount =
            Account(
                id = revalAccountId,
                organizationId = orgId,
                code = "3500",
                name = "Revaluation Reserve",
                type = AccountType.EQUITY,
            )
    }

    @Test
    fun `revalueAsset posts journal entry and updates asset cost for upward revaluation`() {
        whenever(fixedAssetService.getAsset(assetId, orgId)).thenReturn(asset)
        whenever(accountRepository.findAllById(any())).thenReturn(listOf(assetAccount, revalAccount))
        whenever(assetRevaluationRepository.save(any<AssetRevaluation>())).thenAnswer { it.arguments[0] as AssetRevaluation }

        val je =
            JournalEntry(
                id = UUID.randomUUID(),
                entryNumber = "JE-001",
                date = LocalDate.now(),
                description = "",
                organizationId = orgId,
                status = JournalEntryStatus.POSTED,
                source = JournalEntrySource.SYSTEM,
                sourceReference = "",
                lines = emptyList(),
                createdBy = userId,
            )
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any())).thenReturn(je)

        val request =
            AssetRevaluationRequest(
                revaluationDate = LocalDate.now(),
                newCost = BigDecimal("150000.00"),
                revaluationAccountId = revalAccountId,
                reason = "Market appraisal",
            )

        val response = assetRevaluationService.revalueAsset(orgId, assetId, request, userId)

        assertEquals(BigDecimal("100000.00"), response.previousCost)
        assertEquals(BigDecimal("150000.00"), response.newCost)
        assertEquals(BigDecimal("50000.00"), response.revaluationAmount)

        verify(fixedAssetRepository).save(asset)
        assertEquals(BigDecimal("150000.00"), asset.acquisitionCost)

        verify(journalEntryService).createSystemEntry(
            date = eq(request.revaluationDate!!),
            description = any(),
            organizationId = eq(orgId),
            lines = any(),
            sourceReference = any(),
            createdBy = eq(userId),
        )
    }

    @Test
    fun `revalueAsset throws exception if asset is not active`() {
        asset.status = AssetStatus.DISPOSED
        whenever(fixedAssetService.getAsset(assetId, orgId)).thenReturn(asset)

        val request =
            AssetRevaluationRequest(
                revaluationDate = LocalDate.now(),
                newCost = BigDecimal("150000.00"),
                revaluationAccountId = revalAccountId,
                reason = "Market appraisal",
            )

        assertThrows(BusinessRuleException::class.java) {
            assetRevaluationService.revalueAsset(orgId, assetId, request, userId)
        }
    }
}
