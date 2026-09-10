package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateExpenseCategoryRequest
import com.aquinofroilan.tessera.domain.finance.model.ExpenseCategory
import com.aquinofroilan.tessera.domain.finance.repository.ExpenseCategoryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.UUID

class ExpenseCategoryServiceTest {
    private val expenseCategoryRepository: ExpenseCategoryRepository = mock()
    private lateinit var expenseCategoryService: ExpenseCategoryService

    private val orgId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        expenseCategoryService = ExpenseCategoryService(expenseCategoryRepository)
    }

    @Test
    fun `createCategory succeeds`() {
        val request =
            CreateExpenseCategoryRequest(
                name = "Meals",
                expenseAccountId = UUID.randomUUID(),
                policyLimit = BigDecimal("50.00"),
                limitCurrency = "USD",
            )

        whenever(expenseCategoryRepository.findByOrganizationIdAndName(orgId, "Meals")).thenReturn(null)
        whenever(expenseCategoryRepository.save(any<ExpenseCategory>())).thenAnswer { it.arguments[0] as ExpenseCategory }

        val response = expenseCategoryService.createCategory(orgId, userId, request)

        assertEquals("Meals", response.name)
        assertEquals(BigDecimal("50.00"), response.policyLimit)
        assertEquals("USD", response.limitCurrency)
    }

    @Test
    fun `createCategory throws if name exists`() {
        val request =
            CreateExpenseCategoryRequest(
                name = "Meals",
                expenseAccountId = UUID.randomUUID(),
            )

        val existing =
            ExpenseCategory(
                organizationId = orgId,
                name = "Meals",
                expenseAccountId = UUID.randomUUID(),
                createdBy = userId,
            )

        whenever(expenseCategoryRepository.findByOrganizationIdAndName(orgId, "Meals")).thenReturn(existing)

        assertThrows(BusinessRuleException::class.java) {
            expenseCategoryService.createCategory(orgId, userId, request)
        }
    }

    @Test
    fun `createCategory throws if limit set without currency`() {
        val request =
            CreateExpenseCategoryRequest(
                name = "Meals",
                expenseAccountId = UUID.randomUUID(),
                policyLimit = BigDecimal("50.00"),
            )

        whenever(expenseCategoryRepository.findByOrganizationIdAndName(orgId, "Meals")).thenReturn(null)

        assertThrows(BusinessRuleException::class.java) {
            expenseCategoryService.createCategory(orgId, userId, request)
        }
    }
}
