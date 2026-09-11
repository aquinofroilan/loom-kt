package com.aquinofroilan.tessera.domain.finance.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class ForecastStatus {
    DRAFT,
    ACTIVE,
    SUPERSEDED,
    CLOSED,
}

@Entity
@Table(name = "forecast_lines")
class ForecastLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "account_id", columnDefinition = "uuid")
    var accountId: UUID? = null,
    @Column(name = "cost_center_id", columnDefinition = "uuid")
    var costCenterId: UUID? = null,
    @Column(name = "fiscal_period_id", columnDefinition = "uuid")
    var fiscalPeriodId: UUID,
    var amount: BigDecimal = BigDecimal.ZERO,
)

@Entity
@Table(name = "forecasts")
@EntityListeners(AuditingEntityListener::class)
class Forecast(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    var name: String,
    var version: Int = 1,
    @Enumerated(EnumType.STRING)
    var status: ForecastStatus = ForecastStatus.DRAFT,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "forecast_id")
    var lines: MutableList<ForecastLine> = mutableListOf(),
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
