package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.CostCenterResponse
import com.aquinofroilan.tessera.domain.finance.dto.CreateCostCenterRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateCostCenterRequest
import com.aquinofroilan.tessera.domain.finance.service.CostCenterService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/cost-centers")
@Loggable(level = LogLevel.INFO)
class CostCenterController(
    private val costCenterService: CostCenterService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('finance:write')")
    fun createCostCenter(
        @RequestAttribute("organizationId") orgId: UUID,
        @Valid @RequestBody request: CreateCostCenterRequest,
    ): ResponseEntity<CostCenterResponse> {
        val response = costCenterService.createCostCenter(orgId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:write')")
    fun updateCostCenter(
        @RequestAttribute("organizationId") orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCostCenterRequest,
    ): ResponseEntity<CostCenterResponse> {
        val response = costCenterService.updateCostCenter(orgId, id, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:read')")
    fun getCostCenter(
        @RequestAttribute("organizationId") orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<CostCenterResponse> {
        val response = costCenterService.getCostCenterResponse(id, orgId)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance:read')")
    fun listCostCenters(
        @RequestAttribute("organizationId") orgId: UUID,
    ): ResponseEntity<List<CostCenterResponse>> {
        val response = costCenterService.listCostCenters(orgId)
        return ResponseEntity.ok(response)
    }
}
