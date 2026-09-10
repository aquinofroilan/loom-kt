package com.aquinofroilan.tessera.domain.assets.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.assets.dto.AssetRevaluationRequest
import com.aquinofroilan.tessera.domain.assets.service.AssetRevaluationService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/assets/{assetId}/revaluations")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AssetRevaluationController(
    private val assetRevaluationService: AssetRevaluationService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('assets:write')")
    fun revalueAsset(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable assetId: String,
        @Valid @RequestBody request: AssetRevaluationRequest,
    ): ResponseEntity<Any> {
        val assetUuid =
            try {
                UUID.fromString(assetId)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid asset ID"))
            }

        val response = assetRevaluationService.revalueAsset(orgId, assetUuid, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('assets:read')")
    fun listRevaluations(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable assetId: String,
    ): ResponseEntity<Any> {
        val assetUuid =
            try {
                UUID.fromString(assetId)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid asset ID"))
            }

        val response = assetRevaluationService.getAssetRevaluations(orgId, assetUuid)
        return ResponseEntity.ok(response)
    }
}
