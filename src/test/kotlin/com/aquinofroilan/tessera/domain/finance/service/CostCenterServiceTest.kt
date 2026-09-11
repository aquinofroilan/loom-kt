package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateCostCenterRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateCostCenterRequest
import com.aquinofroilan.tessera.domain.finance.model.CostCenter
import com.aquinofroilan.tessera.domain.finance.repository.CostCenterRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

class CostCenterServiceTest {
    private val costCenterRepository: CostCenterRepository = mock()
    private val service = CostCenterService(costCenterRepository)

    @Test
    fun `should create cost center`() {
        val orgId = UUID.randomUUID()
        val request =
            CreateCostCenterRequest(
                code = "IT-01",
                name = "Information Technology",
                description = "IT Department",
            )

        whenever(costCenterRepository.findByOrganizationId(orgId)).thenReturn(emptyList())
        whenever(costCenterRepository.save(any<CostCenter>())).thenAnswer { it.arguments[0] as CostCenter }

        val res = service.createCostCenter(orgId, request)
        assertNotNull(res)
        assertEquals("IT-01", res.code)
        assertEquals("Information Technology", res.name)
    }

    @Test
    fun `should not create duplicate cost center`() {
        val orgId = UUID.randomUUID()
        val request = CreateCostCenterRequest(code = "IT-01", name = "IT")
        val existing =
            CostCenter(
                organizationId = orgId,
                code = "IT-01",
                name = "Old IT",
            )

        whenever(costCenterRepository.findByOrganizationId(orgId)).thenReturn(listOf(existing))

        assertThrows<BusinessRuleException> {
            service.createCostCenter(orgId, request)
        }
    }

    @Test
    fun `should update cost center`() {
        val orgId = UUID.randomUUID()
        val ccId = UUID.randomUUID()
        val existing =
            CostCenter(
                id = ccId,
                organizationId = orgId,
                code = "IT-01",
                name = "IT",
            )
        val request =
            UpdateCostCenterRequest(
                code = "IT-02",
                name = "IT 2.0",
                isActive = false,
            )

        whenever(costCenterRepository.findById(ccId)).thenReturn(Optional.of(existing))
        whenever(costCenterRepository.findByOrganizationId(orgId)).thenReturn(emptyList())
        whenever(costCenterRepository.save(any<CostCenter>())).thenAnswer { it.arguments[0] as CostCenter }

        val res = service.updateCostCenter(orgId, ccId, request)
        assertEquals("IT-02", res.code)
        assertEquals("IT 2.0", res.name)
        assertEquals(false, res.isActive)
    }
}
