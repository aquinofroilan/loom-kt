package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateExpenseCategoryRequest
import com.aquinofroilan.tessera.domain.finance.dto.ExpenseCategoryResponse
import com.aquinofroilan.tessera.domain.finance.dto.UpdateExpenseCategoryRequest
import com.aquinofroilan.tessera.domain.finance.model.ExpenseCategory
import com.aquinofroilan.tessera.domain.finance.repository.ExpenseCategoryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExpenseCategoryService(
    private val expenseCategoryRepository: ExpenseCategoryRepository,
) {
    @Transactional
    fun createCategory(
        organizationId: UUID,
        userId: UUID,
        request: CreateExpenseCategoryRequest,
    ): ExpenseCategoryResponse {
        val existing = expenseCategoryRepository.findByOrganizationIdAndName(organizationId, request.name!!)
        if (existing != null) {
            throw BusinessRuleException("Category with name '${request.name}' already exists")
        }

        if (request.policyLimit != null && request.limitCurrency == null) {
            throw BusinessRuleException("Limit currency is required when policy limit is set")
        }

        val category =
            ExpenseCategory(
                organizationId = organizationId,
                name = request.name,
                description = request.description,
                expenseAccountId = request.expenseAccountId!!,
                policyLimit = request.policyLimit,
                limitCurrency = request.limitCurrency,
                isActive = request.isActive,
                createdBy = userId,
            )

        return mapToResponse(expenseCategoryRepository.save(category))
    }

    @Transactional
    fun updateCategory(
        organizationId: UUID,
        categoryId: UUID,
        request: UpdateExpenseCategoryRequest,
    ): ExpenseCategoryResponse {
        val category = getCategory(categoryId, organizationId)

        val existing = expenseCategoryRepository.findByOrganizationIdAndName(organizationId, request.name!!)
        if (existing != null && existing.id != category.id) {
            throw BusinessRuleException("Category with name '${request.name}' already exists")
        }

        if (request.policyLimit != null && request.limitCurrency == null) {
            throw BusinessRuleException("Limit currency is required when policy limit is set")
        }

        category.name = request.name
        category.description = request.description
        category.expenseAccountId = request.expenseAccountId!!
        category.policyLimit = request.policyLimit
        category.limitCurrency = request.limitCurrency
        if (request.isActive != null) {
            category.isActive = request.isActive
        }

        return mapToResponse(expenseCategoryRepository.save(category))
    }

    fun getCategory(
        categoryId: UUID,
        organizationId: UUID,
    ): ExpenseCategory =
        expenseCategoryRepository
            .findById(categoryId)
            .orElseThrow {
                ResourceNotFoundException("ExpenseCategory not found: $categoryId")
            }.also {
                if (it.organizationId != organizationId) {
                    throw BusinessRuleException("Expense category does not belong to organization")
                }
            }

    fun getCategoryResponse(
        categoryId: UUID,
        organizationId: UUID,
    ): ExpenseCategoryResponse = mapToResponse(getCategory(categoryId, organizationId))

    fun listCategories(
        organizationId: UUID,
        activeOnly: Boolean = false,
    ): List<ExpenseCategoryResponse> {
        val categories = expenseCategoryRepository.findByOrganizationId(organizationId)
        return categories.filter { !activeOnly || it.isActive }.map { mapToResponse(it) }
    }

    private fun mapToResponse(category: ExpenseCategory): ExpenseCategoryResponse =
        ExpenseCategoryResponse(
            id = category.id,
            organizationId = category.organizationId,
            name = category.name,
            description = category.description,
            expenseAccountId = category.expenseAccountId,
            policyLimit = category.policyLimit,
            limitCurrency = category.limitCurrency,
            isActive = category.isActive,
            createdBy = category.createdBy,
            createdAt = category.createdAt?.toString() ?: "",
            updatedAt = category.updatedAt?.toString(),
        )
}
