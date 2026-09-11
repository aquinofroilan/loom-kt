package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.Forecast
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ForecastRepository : JpaRepository<Forecast, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<Forecast>
}
