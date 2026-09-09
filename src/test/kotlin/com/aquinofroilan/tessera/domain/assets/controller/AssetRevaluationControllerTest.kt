package com.aquinofroilan.tessera.domain.assets.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.OrganizationStatusInterceptor
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.config.WebMvcConfig
import com.aquinofroilan.tessera.domain.assets.dto.AssetRevaluationRequest
import com.aquinofroilan.tessera.domain.assets.dto.AssetRevaluationResponse
import com.aquinofroilan.tessera.domain.assets.service.AssetRevaluationService
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [AssetRevaluationController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class AssetRevaluationControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var assetRevaluationService: AssetRevaluationService


    @MockitoBean
    private lateinit var organizationStatusInterceptor: OrganizationStatusInterceptor

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var sessionTokenRepository: SessionTokenRepository

    @MockitoBean
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockitoBean
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @MockitoBean
    private lateinit var invitationRepository: com.aquinofroilan.tessera.domain.auth.repository.InvitationRepository

    @MockitoBean
    private lateinit var organizationRepository: com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository

    @MockitoBean
    private lateinit var authService: com.aquinofroilan.tessera.domain.auth.service.AuthService

    @MockitoBean
    private lateinit var apiKeyService: com.aquinofroilan.tessera.domain.auth.service.ApiKeyService

    @MockitoBean
    private lateinit var rolePermissionCache: com.aquinofroilan.tessera.security.RolePermissionCache

    @MockitoBean
    private lateinit var tokenHasher: com.aquinofroilan.tessera.util.TokenHasher

    @MockitoBean
    private lateinit var accountService: com.aquinofroilan.tessera.domain.finance.service.AccountService

    @MockitoBean
    private lateinit var journalEntryService: com.aquinofroilan.tessera.domain.finance.service.JournalEntryService

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    private val testOrgId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val testUserId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val testAssetId = UUID.randomUUID()
    
    private val testUser = User(
        uuid = testUserId,
        username = "testuser",
        email = "test@example.com",
        passwordHash = "hash",
        firstName = "Test",
        lastName = "User",
        organizationId = testOrgId,
        isActive = true,
    )

    @BeforeEach
    fun setup() {
        val permissionAuthorities = listOf("assets:read", "assets:write").map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, permissionAuthorities)
        authentication.details = SessionContext(
            sessionId = UUID.randomUUID(),
            organizationId = testOrgId,
        )
        SecurityContextHolder.getContext().authentication = authentication
        `when`(authenticationContext.organizationId()).thenReturn(testOrgId)
        `when`(authenticationContext.userId()).thenReturn(testUserId)
        `when`(organizationStatusInterceptor.preHandle(any(), any(), any())).thenReturn(true)
    }

    @Test
    fun `revalueAsset should return 201 Created`() {
        val request = AssetRevaluationRequest(
            revaluationDate = LocalDate.now(),
            newCost = BigDecimal("15000.00"),
            revaluationAccountId = UUID.randomUUID(),
            reason = "Market value adjustment"
        )

        val response = AssetRevaluationResponse(
            id = UUID.randomUUID(),
            assetId = testAssetId,
            revaluationDate = request.revaluationDate.toString(),
            previousCost = BigDecimal("10000.00"),
            newCost = request.newCost!!,
            revaluationAmount = BigDecimal("5000.00"),
            revaluationAccountId = request.revaluationAccountId!!,
            journalEntryId = UUID.randomUUID(),
            reason = "Market value adjustment",
            createdBy = testUserId,
            createdAt = "2023-10-10T10:00:00Z"
        )

        `when`(assetRevaluationService.revalueAsset(any(), any(), any(), any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/assets/$testAssetId/revaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.newCost").value(15000.0))
    }

    @Test
    fun `listRevaluations should return 200 OK`() {
        `when`(assetRevaluationService.getAssetRevaluations(any(), any())).thenReturn(emptyList())

        mockMvc.perform(get("/api/v1/assets/$testAssetId/revaluations"))
            .andExpect(status().isOk)
    }
}
