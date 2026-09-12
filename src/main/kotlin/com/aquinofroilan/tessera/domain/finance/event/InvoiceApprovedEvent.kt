package com.aquinofroilan.tessera.domain.finance.event

import com.aquinofroilan.tessera.domain.finance.model.Invoice

data class InvoiceApprovedEvent(
    val invoice: Invoice,
)
