package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.BudgetLineResponse
import com.aquinofroilan.tessera.domain.finance.dto.BudgetResponse
import com.aquinofroilan.tessera.domain.finance.dto.CreateBudgetRequest
import com.aquinofroilan.tessera.domain.finance.model.Budget
import com.aquinofroilan.tessera.domain.finance.model.BudgetLine
import com.aquinofroilan.tessera.domain.finance.model.BudgetStatus
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.BudgetRepository
import com.aquinofroilan.tessera.domain.finance.repository.CostCenterRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BudgetService(
    private val budgetRepository: BudgetRepository,
    private val fiscalYearService: FiscalYearService,
    private val accountRepository: AccountRepository,
    private val costCenterRepository: CostCenterRepository,
) {
    private val log = LoggerFactory.getLogger(BudgetService::class.java)

    @Transactional
    fun createBudget(
        organizationId: UUID,
        userId: UUID,
        request: CreateBudgetRequest,
    ): BudgetResponse {
        val fiscalYearId = request.fiscalYearId ?: throw BusinessRuleException("Fiscal year ID is required")
        val name = request.name ?: throw BusinessRuleException("Name is required")
        val linesRequest = request.lines ?: throw BusinessRuleException("Lines are required")
        if (linesRequest.isEmpty()) throw BusinessRuleException("At least one line is required")

        val fiscalYear = fiscalYearService.getFiscalYear(fiscalYearId, organizationId)
        val validPeriodIds = fiscalYear.periods.map { it.id }.toSet()

        val existingBudgets = budgetRepository.findByOrganizationIdAndFiscalYearId(organizationId, fiscalYearId)
        val nextVersion = (existingBudgets.maxOfOrNull { it.version } ?: 0) + 1

        val budget =
            Budget(
                organizationId = organizationId,
                fiscalYearId = fiscalYearId,
                name = name,
                version = nextVersion,
                status = BudgetStatus.DRAFT,
                createdBy = userId,
            )

        linesRequest.forEach { lineReq ->
            if (lineReq.accountId == null && lineReq.costCenterId == null) {
                throw BusinessRuleException("Either account ID or cost center ID must be provided")
            }

            if (lineReq.accountId != null) {
                accountRepository.findById(lineReq.accountId).orElseThrow {
                    BusinessRuleException("Account not found: ${lineReq.accountId}")
                }
            }

            if (lineReq.costCenterId != null) {
                costCenterRepository.findById(lineReq.costCenterId).orElseThrow {
                    BusinessRuleException("Cost center not found: ${lineReq.costCenterId}")
                }
            }

            val periodId = lineReq.fiscalPeriodId ?: throw BusinessRuleException("Fiscal period ID is required")
            if (!validPeriodIds.contains(periodId)) {
                throw BusinessRuleException("Fiscal period $periodId does not belong to fiscal year $fiscalYearId")
            }

            budget.lines.add(
                BudgetLine(
                    accountId = lineReq.accountId,
                    costCenterId = lineReq.costCenterId,
                    fiscalPeriodId = periodId,
                    amount = lineReq.amount ?: throw BusinessRuleException("Amount is required"),
                ),
            )
        }

        val saved = budgetRepository.save(budget)
        log.info("Created budget {} (version {}) for fiscal year {}", saved.id, nextVersion, fiscalYearId)
        return mapToResponse(saved)
    }

    @Transactional
    fun activateBudget(
        organizationId: UUID,
        budgetId: UUID,
        userId: UUID,
    ): BudgetResponse {
        val budget = getBudget(budgetId, organizationId)
        if (budget.status != BudgetStatus.DRAFT) {
            throw BusinessRuleException("Only draft budgets can be activated")
        }

        budget.status = BudgetStatus.ACTIVE
        val saved = budgetRepository.save(budget)
        log.info("Activated budget {}", saved.id)
        return mapToResponse(saved)
    }

    @Transactional
    fun closeBudget(
        organizationId: UUID,
        budgetId: UUID,
        userId: UUID,
    ): BudgetResponse {
        val budget = getBudget(budgetId, organizationId)
        if (budget.status == BudgetStatus.CLOSED) {
            throw BusinessRuleException("Budget is already closed")
        }

        budget.status = BudgetStatus.CLOSED
        val saved = budgetRepository.save(budget)
        log.info("Closed budget {}", saved.id)
        return mapToResponse(saved)
    }

    fun getBudget(
        budgetId: UUID,
        organizationId: UUID,
    ): Budget =
        budgetRepository
            .findById(budgetId)
            .orElseThrow { ResourceNotFoundException("Budget not found: $budgetId") }
            .also {
                if (it.organizationId != organizationId) {
                    throw BusinessRuleException("Budget does not belong to organization")
                }
            }

    fun getBudgetResponse(
        budgetId: UUID,
        organizationId: UUID,
    ): BudgetResponse = mapToResponse(getBudget(budgetId, organizationId))

    fun listBudgets(
        organizationId: UUID,
        fiscalYearId: UUID,
    ): List<BudgetResponse> =
        budgetRepository
            .findByOrganizationIdAndFiscalYearId(organizationId, fiscalYearId)
            .map { mapToResponse(it) }

    private fun mapToResponse(budget: Budget): BudgetResponse =
        BudgetResponse(
            id = budget.id,
            organizationId = budget.organizationId,
            fiscalYearId = budget.fiscalYearId,
            name = budget.name,
            version = budget.version,
            status = budget.status,
            lines =
                budget.lines.map { line ->
                    BudgetLineResponse(
                        id = line.id,
                        accountId = line.accountId,
                        costCenterId = line.costCenterId,
                        fiscalPeriodId = line.fiscalPeriodId,
                        amount = line.amount,
                    )
                },
            createdBy = budget.createdBy,
            createdAt = budget.createdAt?.toString() ?: "",
            updatedAt = budget.updatedAt?.toString(),
        )
}
