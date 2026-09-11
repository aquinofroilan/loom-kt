package com.aquinofroilan.tessera.domain.finance.repository

import java.math.BigDecimal
import java.util.UUID

interface ActualAggregationProjection {
    fun getAccountId(): UUID?

    fun getCostCenterId(): UUID?

    fun getNetBalance(): BigDecimal
}
