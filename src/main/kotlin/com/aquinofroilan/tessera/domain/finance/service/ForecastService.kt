package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateForecastRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateForecastRequest
import com.aquinofroilan.tessera.domain.finance.model.Forecast
import com.aquinofroilan.tessera.domain.finance.model.ForecastLine
import com.aquinofroilan.tessera.domain.finance.model.ForecastStatus
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.CostCenterRepository
import com.aquinofroilan.tessera.domain.finance.repository.ForecastRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ForecastService(
    private val forecastRepository: ForecastRepository,
    private val accountRepository: AccountRepository,
    private val costCenterRepository: CostCenterRepository,
) {
    @Transactional
    fun createForecast(
        request: CreateForecastRequest,
        organizationId: UUID,
        createdBy: UUID,
    ): Forecast {
        validateLines(request.lines.map { Triple(it.accountId, it.costCenterId, it.fiscalPeriodId) }, organizationId)

        val forecast =
            Forecast(
                organizationId = organizationId,
                name = request.name,
                createdBy = createdBy,
            )

        forecast.lines.addAll(
            request.lines.map {
                ForecastLine(
                    accountId = it.accountId,
                    costCenterId = it.costCenterId,
                    fiscalPeriodId = it.fiscalPeriodId,
                    amount = it.amount ?: throw BusinessRuleException("Amount is required"),
                )
            },
        )

        return forecastRepository.save(forecast)
    }

    @Transactional
    fun updateForecast(
        id: UUID,
        request: UpdateForecastRequest,
        organizationId: UUID,
    ): Forecast {
        val forecast = getForecast(id, organizationId)
        if (forecast.status != ForecastStatus.DRAFT) {
            throw BusinessRuleException("Only draft forecasts can be updated")
        }

        validateLines(request.lines.map { Triple(it.accountId, it.costCenterId, it.fiscalPeriodId) }, organizationId)

        forecast.name = request.name
        forecast.version += 1

        val newLinesMap = request.lines.filter { it.id != null }.associateBy { it.id }

        forecast.lines.removeIf { it.id !in newLinesMap }

        forecast.lines.forEach { line ->
            newLinesMap[line.id]?.let { reqLine ->
                line.accountId = reqLine.accountId
                line.costCenterId = reqLine.costCenterId
                line.fiscalPeriodId = reqLine.fiscalPeriodId
                line.amount = reqLine.amount ?: throw BusinessRuleException("Amount is required")
            }
        }

        val newLines = request.lines.filter { it.id == null }
        forecast.lines.addAll(
            newLines.map {
                ForecastLine(
                    accountId = it.accountId,
                    costCenterId = it.costCenterId,
                    fiscalPeriodId = it.fiscalPeriodId,
                    amount = it.amount ?: throw BusinessRuleException("Amount is required"),
                )
            },
        )

        return forecastRepository.save(forecast)
    }

    fun getForecast(
        id: UUID,
        organizationId: UUID,
    ): Forecast {
        val forecast =
            forecastRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Forecast not found")
            }
        if (forecast.organizationId != organizationId) {
            throw ResourceNotFoundException("Forecast not found")
        }
        return forecast
    }

    fun listForecasts(organizationId: UUID): List<Forecast> = forecastRepository.findByOrganizationId(organizationId)

    @Transactional
    fun activateForecast(
        id: UUID,
        organizationId: UUID,
    ): Forecast {
        val forecast = getForecast(id, organizationId)
        if (forecast.status != ForecastStatus.DRAFT) {
            throw BusinessRuleException("Only draft forecasts can be activated")
        }

        // When activating a forecast, we might supersede other active forecasts if they overlap?
        // For simplicity, we just mark this as ACTIVE. Users can manually supersede others if they want.
        forecast.status = ForecastStatus.ACTIVE
        return forecastRepository.save(forecast)
    }

    @Transactional
    fun supersedeForecast(
        id: UUID,
        organizationId: UUID,
    ): Forecast {
        val forecast = getForecast(id, organizationId)
        if (forecast.status != ForecastStatus.ACTIVE) {
            throw BusinessRuleException("Only active forecasts can be superseded")
        }
        forecast.status = ForecastStatus.SUPERSEDED
        return forecastRepository.save(forecast)
    }

    private fun validateLines(
        lines: List<Triple<UUID?, UUID?, UUID>>,
        organizationId: UUID,
    ) {
        val accountIds = lines.mapNotNull { it.first }.toSet()
        if (accountIds.isNotEmpty()) {
            val accounts = accountRepository.findAllById(accountIds).filter { it.organizationId == organizationId }
            if (accounts.size != accountIds.size) {
                throw BusinessRuleException("One or more accounts are invalid or do not belong to the organization")
            }
        }

        val costCenterIds = lines.mapNotNull { it.second }.toSet()
        if (costCenterIds.isNotEmpty()) {
            val costCenters = costCenterRepository.findAllById(costCenterIds).filter { it.organizationId == organizationId }
            if (costCenters.size != costCenterIds.size) {
                throw BusinessRuleException("One or more cost centers are invalid or do not belong to the organization")
            }
        }
    }
}
