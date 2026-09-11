package com.aquinofroilan.tessera.domain.finance.repository

import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.LocalDate

data class AccountTotals(
    val totalDebits: BigDecimal,
    val totalCredits: BigDecimal,
)

interface JournalEntryAggregations {
    fun aggregateAccountTotals(
        organizationId: java.util.UUID,
        accountIds: Collection<java.util.UUID>?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Map<java.util.UUID, AccountTotals>

    fun aggregateBudgetActuals(
        organizationId: java.util.UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BudgetActualAggregation>
}

data class BudgetActualAggregation(
    val accountId: java.util.UUID?,
    val costCenterId: java.util.UUID?,
    val totalDebits: BigDecimal,
    val totalCredits: BigDecimal,
)

open class JournalEntryAggregationsImpl(
    private val jdbc: JdbcTemplate,
) : JournalEntryAggregations {
    override fun aggregateAccountTotals(
        organizationId: java.util.UUID,
        accountIds: Collection<java.util.UUID>?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Map<java.util.UUID, AccountTotals> {
        if (accountIds != null && accountIds.isEmpty()) {
            return emptyMap()
        }

        val params = mutableListOf<Any>()
        params.add(organizationId)

        val sql =
            buildString {
                append(
                    """
                    SELECT l.account_id,
                           COALESCE(SUM(l.debit), 0)  AS total_debits,
                           COALESCE(SUM(l.credit), 0) AS total_credits
                      FROM journal_entry_lines l
                      JOIN journal_entries e ON e.id = l.journal_entry_id
                     WHERE e.organization_id = ?::uuid
                       AND e.status IN ('POSTED', 'VOIDED')
                    """.trimIndent(),
                )
                if (startDate != null) {
                    append(" AND e.date >= ?")
                    params.add(startDate)
                }
                if (endDate != null) {
                    append(" AND e.date <= ?")
                    params.add(endDate)
                }
                if (accountIds != null) {
                    val placeholders = accountIds.joinToString(",") { "?::uuid" }
                    append(" AND l.account_id IN ($placeholders)")
                    params.addAll(accountIds)
                }
                append(" GROUP BY l.account_id")
            }

        val totals = mutableMapOf<java.util.UUID, AccountTotals>()
        jdbc.query(sql, { rs ->
            val accountId = java.util.UUID.fromString(rs.getString("account_id"))
            val debits = rs.getBigDecimal("total_debits") ?: BigDecimal.ZERO
            val credits = rs.getBigDecimal("total_credits") ?: BigDecimal.ZERO
            totals[accountId] = AccountTotals(debits, credits)
        }, *params.toTypedArray())

        return totals
    }

    override fun aggregateBudgetActuals(
        organizationId: java.util.UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BudgetActualAggregation> {
        val sql =
            """
            SELECT l.account_id,
                   l.cost_center_id,
                   COALESCE(SUM(l.debit), 0)  AS total_debits,
                   COALESCE(SUM(l.credit), 0) AS total_credits
              FROM journal_entry_lines l
              JOIN journal_entries e ON e.id = l.journal_entry_id
             WHERE e.organization_id = ?::uuid
               AND e.status IN ('POSTED', 'VOIDED')
               AND e.date >= ?
               AND e.date <= ?
             GROUP BY l.account_id, l.cost_center_id
            """.trimIndent()

        val results = mutableListOf<BudgetActualAggregation>()
        jdbc.query(sql, { rs ->
            val accountIdStr = rs.getString("account_id")
            val costCenterIdStr = rs.getString("cost_center_id")
            val debits = rs.getBigDecimal("total_debits") ?: BigDecimal.ZERO
            val credits = rs.getBigDecimal("total_credits") ?: BigDecimal.ZERO

            results.add(
                BudgetActualAggregation(
                    accountId = accountIdStr?.let { java.util.UUID.fromString(it) },
                    costCenterId = costCenterIdStr?.let { java.util.UUID.fromString(it) },
                    totalDebits = debits,
                    totalCredits = credits,
                ),
            )
        }, organizationId, startDate, endDate)

        return results
    }
}
