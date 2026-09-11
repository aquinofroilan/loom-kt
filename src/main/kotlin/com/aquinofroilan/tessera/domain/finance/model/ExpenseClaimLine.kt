package com.aquinofroilan.tessera.domain.finance.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "expense_claim_lines")
data class ExpenseClaimLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "line_number")
    var lineNumber: Int,
    @Column(name = "expense_date")
    var expenseDate: LocalDate,
    var category: String,
    @Column(name = "category_id", columnDefinition = "uuid")
    var categoryId: UUID? = null,
    var description: String? = null,
    @Column(name = "original_currency")
    var originalCurrency: String,
    @Column(name = "original_amount")
    var originalAmount: BigDecimal,
    @Column(name = "exchange_rate")
    var exchangeRate: BigDecimal = BigDecimal.ONE,
    @Column(name = "reimbursement_amount")
    var reimbursementAmount: BigDecimal,
    @Column(name = "project_id", columnDefinition = "uuid")
    var projectId: UUID? = null,
    @Column(name = "receipt_url")
    var receiptUrl: String? = null,
)
