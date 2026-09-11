package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.domain.finance.dto.CreateForecastRequest
import com.aquinofroilan.tessera.domain.finance.dto.ForecastResponse
import com.aquinofroilan.tessera.domain.finance.dto.UpdateForecastRequest
import com.aquinofroilan.tessera.domain.finance.service.ForecastService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/forecasts")
class ForecastController(
    private val forecastService: ForecastService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createForecast(
        @Valid @RequestBody request: CreateForecastRequest,
        @RequestHeader("X-Organization-Id") organizationId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ForecastResponse {
        val forecast = forecastService.createForecast(request, organizationId, userId)
        return ForecastResponse.from(forecast)
    }

    @PutMapping("/{id}")
    fun updateForecast(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateForecastRequest,
        @RequestHeader("X-Organization-Id") organizationId: UUID,
    ): ForecastResponse {
        val forecast = forecastService.updateForecast(id, request, organizationId)
        return ForecastResponse.from(forecast)
    }

    @GetMapping("/{id}")
    fun getForecast(
        @PathVariable id: UUID,
        @RequestHeader("X-Organization-Id") organizationId: UUID,
    ): ForecastResponse {
        val forecast = forecastService.getForecast(id, organizationId)
        return ForecastResponse.from(forecast)
    }

    @GetMapping
    fun listForecasts(
        @RequestHeader("X-Organization-Id") organizationId: UUID,
    ): List<ForecastResponse> = forecastService.listForecasts(organizationId).map { ForecastResponse.from(it) }

    @PostMapping("/{id}/activate")
    fun activateForecast(
        @PathVariable id: UUID,
        @RequestHeader("X-Organization-Id") organizationId: UUID,
    ): ForecastResponse {
        val forecast = forecastService.activateForecast(id, organizationId)
        return ForecastResponse.from(forecast)
    }

    @PostMapping("/{id}/supersede")
    fun supersedeForecast(
        @PathVariable id: UUID,
        @RequestHeader("X-Organization-Id") organizationId: UUID,
    ): ForecastResponse {
        val forecast = forecastService.supersedeForecast(id, organizationId)
        return ForecastResponse.from(forecast)
    }
}
