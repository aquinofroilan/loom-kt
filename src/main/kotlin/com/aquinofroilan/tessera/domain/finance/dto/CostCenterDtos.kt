package com.aquinofroilan.tessera.domain.finance.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateCostCenterRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 50)
    val code: String?,
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255)
    val name: String?,
    val description: String? = null,
)

data class UpdateCostCenterRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 50)
    val code: String?,
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255)
    val name: String?,
    val description: String? = null,
    val isActive: Boolean? = true,
)

data class CostCenterResponse(
    val id: UUID,
    val organizationId: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String?,
)
