package com.aquinofroilan.tessera.domain.finance.dto

import com.aquinofroilan.tessera.domain.finance.model.Forecast
import com.aquinofroilan.tessera.domain.finance.model.ForecastStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class CreateForecastLineRequest(
    val accountId: UUID? = null,
    val costCenterId: UUID? = null,
    @field:NotNull(message = "Fiscal period ID is required")
    val fiscalPeriodId: UUID,
    @field:NotNull(message = "Amount is required")
    val amount: BigDecimal?,
)

data class CreateForecastRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:NotEmpty(message = "At least one forecast line is required")
    @field:Valid
    val lines: List<CreateForecastLineRequest>,
)

data class UpdateForecastLineRequest(
    val id: UUID? = null,
    val accountId: UUID? = null,
    val costCenterId: UUID? = null,
    @field:NotNull(message = "Fiscal period ID is required")
    val fiscalPeriodId: UUID,
    @field:NotNull(message = "Amount is required")
    val amount: BigDecimal?,
)

data class UpdateForecastRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:NotEmpty(message = "At least one forecast line is required")
    @field:Valid
    val lines: List<UpdateForecastLineRequest>,
)

data class ForecastLineResponse(
    val id: UUID,
    val accountId: UUID?,
    val costCenterId: UUID?,
    val fiscalPeriodId: UUID,
    val amount: BigDecimal,
)

data class ForecastResponse(
    val id: UUID,
    val organizationId: UUID,
    val name: String,
    val version: Int,
    val status: ForecastStatus,
    val lines: List<ForecastLineResponse>,
    val createdBy: UUID,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(forecast: Forecast) =
            ForecastResponse(
                id = forecast.id,
                organizationId = forecast.organizationId,
                name = forecast.name,
                version = forecast.version,
                status = forecast.status,
                lines =
                    forecast.lines.map {
                        ForecastLineResponse(
                            id = it.id,
                            accountId = it.accountId,
                            costCenterId = it.costCenterId,
                            fiscalPeriodId = it.fiscalPeriodId,
                            amount = it.amount,
                        )
                    },
                createdBy = forecast.createdBy,
                createdAt = forecast.createdAt?.toString(),
                updatedAt = forecast.updatedAt?.toString(),
            )
    }
}
