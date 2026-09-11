package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateForecastLineRequest
import com.aquinofroilan.tessera.domain.finance.dto.CreateForecastRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateForecastLineRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateForecastRequest
import com.aquinofroilan.tessera.domain.finance.model.Forecast
import com.aquinofroilan.tessera.domain.finance.model.ForecastLine
import com.aquinofroilan.tessera.domain.finance.model.ForecastStatus
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.CostCenterRepository
import com.aquinofroilan.tessera.domain.finance.repository.ForecastRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class ForecastServiceTest {
    private val forecastRepository: ForecastRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val costCenterRepository: CostCenterRepository = mock()
    private lateinit var service: ForecastService

    private val orgId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val periodId = UUID.randomUUID()
    private val accountId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        whenever(forecastRepository.save(any<Forecast>())).thenAnswer { it.arguments[0] }
        whenever(accountRepository.findAllById(any<Iterable<UUID>>())).thenReturn(
            listOf(
                com.aquinofroilan.tessera.domain.finance.model.Account(
                    id = accountId,
                    organizationId = orgId,
                    name = "Test",
                    code = "1000",
                    type = com.aquinofroilan.tessera.domain.finance.model.AccountType.ASSET,
                ),
            ),
        )
        service =
            ForecastService(
                forecastRepository,
                accountRepository,
                costCenterRepository,
            )
    }

    @Test
    fun `create forecast`() {
        val req =
            CreateForecastRequest(
                name = "Q3 Forecast",
                lines =
                    listOf(
                        CreateForecastLineRequest(
                            accountId = accountId,
                            fiscalPeriodId = periodId,
                            amount = BigDecimal("1000.00"),
                        ),
                    ),
            )
        val forecast = service.createForecast(req, orgId, userId)
        assertThat(forecast.name).isEqualTo("Q3 Forecast")
        assertThat(forecast.status).isEqualTo(ForecastStatus.DRAFT)
        assertThat(forecast.lines).hasSize(1)
        assertThat(forecast.lines[0].amount).isEqualByComparingTo("1000.00")
    }

    @Test
    fun `update forecast`() {
        val forecast =
            Forecast(
                organizationId = orgId,
                name = "Q3",
                createdBy = userId,
            )
        val line =
            ForecastLine(
                accountId = accountId,
                fiscalPeriodId = periodId,
                amount = BigDecimal("500.00"),
            )
        forecast.lines.add(line)

        whenever(forecastRepository.findById(forecast.id)).thenReturn(Optional.of(forecast))

        val req =
            UpdateForecastRequest(
                name = "Q3 Revised",
                lines =
                    listOf(
                        UpdateForecastLineRequest(
                            id = line.id,
                            accountId = accountId,
                            fiscalPeriodId = periodId,
                            amount = BigDecimal("800.00"),
                        ),
                        UpdateForecastLineRequest(
                            accountId = accountId,
                            fiscalPeriodId = UUID.randomUUID(),
                            amount = BigDecimal("200.00"),
                        ),
                    ),
            )

        val updated = service.updateForecast(forecast.id, req, orgId)
        assertThat(updated.name).isEqualTo("Q3 Revised")
        assertThat(updated.version).isEqualTo(2)
        assertThat(updated.lines).hasSize(2)
        val l1 = updated.lines.find { it.id == line.id }!!
        assertThat(l1.amount).isEqualByComparingTo("800.00")
    }

    @Test
    fun `activate forecast`() {
        val forecast =
            Forecast(
                organizationId = orgId,
                name = "Q3",
                createdBy = userId,
            )
        whenever(forecastRepository.findById(forecast.id)).thenReturn(Optional.of(forecast))

        val activated = service.activateForecast(forecast.id, orgId)
        assertThat(activated.status).isEqualTo(ForecastStatus.ACTIVE)
    }
}
