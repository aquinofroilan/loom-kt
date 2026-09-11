package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CostCenterResponse
import com.aquinofroilan.tessera.domain.finance.dto.CreateCostCenterRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateCostCenterRequest
import com.aquinofroilan.tessera.domain.finance.model.CostCenter
import com.aquinofroilan.tessera.domain.finance.repository.CostCenterRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CostCenterService(
    private val costCenterRepository: CostCenterRepository,
) {
    private val log = LoggerFactory.getLogger(CostCenterService::class.java)

    @Transactional
    fun createCostCenter(
        organizationId: UUID,
        request: CreateCostCenterRequest,
    ): CostCenterResponse {
        val code = request.code ?: throw BusinessRuleException("Code is required")
        val name = request.name ?: throw BusinessRuleException("Name is required")

        val existing = costCenterRepository.findByOrganizationId(organizationId).find { it.code == code }
        if (existing != null) {
            throw BusinessRuleException("Cost center with code $code already exists")
        }

        val costCenter =
            CostCenter(
                organizationId = organizationId,
                code = code,
                name = name,
                description = request.description,
            )

        val saved = costCenterRepository.save(costCenter)
        log.info("Created cost center {} for organization {}", saved.id, organizationId)
        return mapToResponse(saved)
    }

    @Transactional
    fun updateCostCenter(
        organizationId: UUID,
        costCenterId: UUID,
        request: UpdateCostCenterRequest,
    ): CostCenterResponse {
        val costCenter = getCostCenter(costCenterId, organizationId)

        val code = request.code ?: throw BusinessRuleException("Code is required")
        if (costCenter.code != code) {
            val existing = costCenterRepository.findByOrganizationId(organizationId).find { it.code == code }
            if (existing != null) {
                throw BusinessRuleException("Cost center with code $code already exists")
            }
        }

        costCenter.code = code
        costCenter.name = request.name ?: throw BusinessRuleException("Name is required")
        costCenter.description = request.description
        if (request.isActive != null) {
            costCenter.isActive = request.isActive
        }

        val saved = costCenterRepository.save(costCenter)
        log.info("Updated cost center {} for organization {}", saved.id, organizationId)
        return mapToResponse(saved)
    }

    fun getCostCenter(
        costCenterId: UUID,
        organizationId: UUID,
    ): CostCenter =
        costCenterRepository
            .findById(costCenterId)
            .orElseThrow { ResourceNotFoundException("Cost center not found: $costCenterId") }
            .also {
                if (it.organizationId != organizationId) {
                    throw BusinessRuleException("Cost center does not belong to organization")
                }
            }

    fun getCostCenterResponse(
        costCenterId: UUID,
        organizationId: UUID,
    ): CostCenterResponse = mapToResponse(getCostCenter(costCenterId, organizationId))

    fun listCostCenters(organizationId: UUID): List<CostCenterResponse> =
        costCenterRepository
            .findByOrganizationId(organizationId)
            .map { mapToResponse(it) }

    private fun mapToResponse(costCenter: CostCenter): CostCenterResponse =
        CostCenterResponse(
            id = costCenter.id,
            organizationId = costCenter.organizationId,
            code = costCenter.code,
            name = costCenter.name,
            description = costCenter.description,
            isActive = costCenter.isActive,
            createdAt = costCenter.createdAt?.toString() ?: "",
            updatedAt = costCenter.updatedAt?.toString(),
        )
}
