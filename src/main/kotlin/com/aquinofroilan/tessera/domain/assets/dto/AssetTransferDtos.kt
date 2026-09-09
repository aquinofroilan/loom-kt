package com.aquinofroilan.tessera.domain.assets.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

data class AssetTransferRequest(
    @field:NotNull(message = "Transfer date is required")
    val transferDate: LocalDate?,
    @field:NotBlank(message = "To location is required")
    @field:Size(max = 200)
    val toLocation: String?,
    @field:Size(max = 1000)
    val reason: String? = null,
)

data class AssetTransferResponse(
    val id: UUID,
    val assetId: UUID,
    val transferDate: String,
    val fromLocation: String?,
    val toLocation: String,
    val reason: String?,
    val createdBy: UUID,
    val createdAt: String,
)
