package com.aquinofroilan.tessera.domain.inventory.repository

import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.UUID

interface StockOnHandQueries {
    fun applyDelta(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        binId: java.util.UUID? = null,
        delta: BigDecimal,
        allowNegative: Boolean,
    ): Boolean

    fun get(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        binId: java.util.UUID? = null,
    ): BigDecimal
}

open class StockOnHandQueriesImpl(
    private val jdbc: JdbcTemplate,
) : StockOnHandQueries {
    override fun applyDelta(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        binId: java.util.UUID?,
        delta: BigDecimal,
        allowNegative: Boolean,
    ): Boolean {
        if (delta.signum() == 0) return true

        val binCondition = if (binId == null) "AND bin_id IS NULL" else "AND bin_id = ?::uuid"

        if (!allowNegative && delta.signum() < 0) {
            val sql =
                """
                UPDATE stock_on_hand
                   SET quantity = quantity + ?,
                       updated_at = current_timestamp
                 WHERE organization_id = ?::uuid
                   AND product_id = ?::uuid
                   AND warehouse_id = ?::uuid
                   $binCondition
                   AND quantity >= ?
                """.trimIndent()

            val args =
                if (binId == null) {
                    arrayOf(delta, organizationId, productId, warehouseId, delta.negate())
                } else {
                    arrayOf(delta, organizationId, productId, warehouseId, binId, delta.negate())
                }
            val updated = jdbc.update(sql, *args)
            return updated > 0
        }

        val sql =
            """
            INSERT INTO stock_on_hand (id, organization_id, product_id, warehouse_id, bin_id, quantity, created_at, updated_at)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, ${if (binId == null) "NULL" else "?::uuid"}, ?, current_timestamp, current_timestamp)
            ON CONFLICT (organization_id, product_id, warehouse_id, COALESCE(bin_id, '00000000-0000-0000-0000-000000000000'::uuid))
            DO UPDATE SET quantity = stock_on_hand.quantity + EXCLUDED.quantity,
                          updated_at = current_timestamp
            """.trimIndent()

        val id = java.util.UUID.ofEpochMillis(System.currentTimeMillis())
        val args =
            if (binId == null) {
                arrayOf(id, organizationId, productId, warehouseId, delta)
            } else {
                arrayOf(id, organizationId, productId, warehouseId, binId, delta)
            }
        val updated = jdbc.update(sql, *args)
        return updated > 0
    }

    override fun get(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        binId: java.util.UUID?,
    ): BigDecimal {
        val binCondition = if (binId == null) "AND bin_id IS NULL" else "AND bin_id = ?::uuid"
        val sql =
            """
            SELECT quantity FROM stock_on_hand
             WHERE organization_id = ?::uuid
               AND product_id = ?::uuid
               AND warehouse_id = ?::uuid
               $binCondition
            """.trimIndent()

        val args =
            if (binId == null) {
                arrayOf(organizationId, productId, warehouseId)
            } else {
                arrayOf(organizationId, productId, warehouseId, binId)
            }
        val rows = jdbc.queryForList(sql, BigDecimal::class.java, *args)
        return rows.firstOrNull() ?: BigDecimal.ZERO
    }
}
