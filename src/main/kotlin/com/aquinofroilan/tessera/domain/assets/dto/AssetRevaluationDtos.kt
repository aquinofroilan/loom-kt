package com.aquinofroilan.tessera.domain.assets.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class AssetRevaluationRequest(
    @field:NotNull(message = "Revaluation date is required")
    val revaluationDate: LocalDate?,
    @field:NotNull(message = "New cost is required")
    @field:PositiveOrZero(message = "New cost must be positive or zero")
    val newCost: BigDecimal?,
    @field:NotNull(message = "Revaluation account ID is required")
    val revaluationAccountId: UUID?,
    @field:Size(max = 1000)
    val reason: String? = null,
)

data class AssetRevaluationResponse(
    val id: UUID,
    val assetId: UUID,
    val revaluationDate: String,
    val previousCost: BigDecimal,
    val newCost: BigDecimal,
    val revaluationAmount: BigDecimal,
    val revaluationAccountId: UUID,
    val journalEntryId: UUID?,
    val reason: String?,
    val createdBy: UUID,
    val createdAt: String,
)
