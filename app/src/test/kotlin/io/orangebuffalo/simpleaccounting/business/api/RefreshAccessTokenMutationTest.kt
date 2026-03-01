package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration

class RefreshAccessTokenMutationTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @MockitoBean
    lateinit var jwtService: JwtService

    @MockitoBean
    lateinit var refreshTokensService: RefreshTokensService

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val fryWorkspace = workspace(owner = fry)
            val revokedAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                revoked = true,
                validTill = MOCK_TIME.plus(Duration.ofDays(100)),
                token = "revokedToken"
            )
            val expiredAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                revoked = false,
                validTill = MOCK_TIME.minusMillis(1),
                token = "expiredToken"
            )
            val validAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                revoked = false,
                validTill = MOCK_TIME.plus(Duration.ofDays(42)),
                token = "validToken"
            )
        }
    }

    @Test
    fun `should return null token when user is not authenticated and no refresh token`() {
        client
            .graphqlMutation { refreshAccessTokenMutation() }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.RefreshAccessToken to buildJsonObject {
                    put("accessToken", JsonNull)
                }
            )
    }

    @Test
    suspend fun `should return JWT token when token endpoint is hit and cookie is valid`() {
        val principal = preconditions.fry.toSecurityPrincipal()

        whenever(jwtService.buildJwtToken(principal)) doReturn "jwtTokenForFry"
        whenever(refreshTokensService.validateTokenAndBuildUserDetails("refreshTokenForFry")) doReturn principal

        client
            .graphqlMutation { refreshAccessTokenMutation() }
            .fromAnonymous()
            .cookie("refreshToken", "refreshTokenForFry")
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.RefreshAccessToken to buildJsonObject {
                    put("accessToken", "jwtTokenForFry")
                }
            )
    }

    @Test
    suspend fun `should return JWT token when token endpoint is hit and cookie is valid for admin user`() {
        val principal = preconditions.farnsworth.toSecurityPrincipal()

        whenever(jwtService.buildJwtToken(principal)) doReturn "jwtTokenForFarnsworth"
        whenever(refreshTokensService.validateTokenAndBuildUserDetails("refreshTokenForFarnsworth")) doReturn principal

        client
            .graphqlMutation { refreshAccessTokenMutation() }
            .fromAnonymous()
            .cookie("refreshToken", "refreshTokenForFarnsworth")
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.RefreshAccessToken to buildJsonObject {
                    put("accessToken", "jwtTokenForFarnsworth")
                }
            )
    }

    @Test
    suspend fun `should return JWT token when user is authenticated with regular user`() {
        val principal = preconditions.fry.toSecurityPrincipal()

        whenever(jwtService.buildJwtToken(principal)) doReturn "jwtTokenForFry"

        client
            .graphqlMutation { refreshAccessTokenMutation() }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.RefreshAccessToken to buildJsonObject {
                    put("accessToken", "jwtTokenForFry")
                }
            )
    }

    @Test
    suspend fun `should return JWT token when user is authenticated with admin user`() {
        val principal = preconditions.farnsworth.toSecurityPrincipal()

        whenever(jwtService.buildJwtToken(principal)) doReturn "jwtTokenForFarnsworth"

        client
            .graphqlMutation { refreshAccessTokenMutation() }
            .from(preconditions.farnsworth)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.RefreshAccessToken to buildJsonObject {
                    put("accessToken", "jwtTokenForFarnsworth")
                }
            )
    }

    @Test
    fun `should return JWT token when user is authenticated with transient user`() {
        // Force preconditions evaluation before stubbing
        val tokenValue = preconditions.validAccessToken.token
        val validTill = preconditions.validAccessToken.validTill

        // Mock JWT validation for the valid token
        whenever(jwtService.validateTokenAndBuildUserDetails(any())) doReturn
                createTransientUserPrincipal(tokenValue)

        whenever(jwtService.buildJwtToken(argThat {
            userName == "validToken" && isTransient
        }, eq(validTill))) doReturn "jwtTokenForTransientUser"

        client
            .graphqlMutation { refreshAccessTokenMutation() }
            .usingSharedWorkspaceToken(tokenValue)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.RefreshAccessToken to buildJsonObject {
                    put("accessToken", "jwtTokenForTransientUser")
                }
            )
    }

    @Test
    fun `should return null token when transient user token is revoked`() {
        // Force preconditions evaluation before stubbing
        val tokenValue = preconditions.revokedAccessToken.token

        // Mock JWT validation for the revoked token
        whenever(jwtService.validateTokenAndBuildUserDetails(any())) doReturn
                createTransientUserPrincipal(tokenValue)

        client
            .graphqlMutation { refreshAccessTokenMutation() }
            .usingSharedWorkspaceToken(tokenValue)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.RefreshAccessToken to buildJsonObject {
                    put("accessToken", JsonNull)
                }
            )
    }

    @Test
    fun `should return null token when transient user token is expired`() {
        // Force preconditions evaluation before stubbing
        val tokenValue = preconditions.expiredAccessToken.token

        // Mock JWT validation for the expired token
        whenever(jwtService.validateTokenAndBuildUserDetails(any())) doReturn
                createTransientUserPrincipal(tokenValue)

        client
            .graphqlMutation { refreshAccessTokenMutation() }
            .usingSharedWorkspaceToken(tokenValue)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.RefreshAccessToken to buildJsonObject {
                    put("accessToken", JsonNull)
                }
            )
    }

    private fun MutationProjection.refreshAccessTokenMutation(): MutationProjection = refreshAccessToken {
        accessToken
    }
}
