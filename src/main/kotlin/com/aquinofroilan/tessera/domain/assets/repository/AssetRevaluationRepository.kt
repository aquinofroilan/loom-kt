package com.aquinofroilan.tessera.domain.assets.repository

import com.aquinofroilan.tessera.domain.assets.model.AssetRevaluation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AssetRevaluationRepository : JpaRepository<AssetRevaluation, UUID> {
    fun findByOrganizationIdAndAssetId(
        organizationId: UUID,
        assetId: UUID,
    ): List<AssetRevaluation>
}
