package io.orangebuffalo.simpleaccounting.business.api.auth

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.security.SaUserRoles
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Duration

class CreateAccessTokenByWorkspaceAccessTokenMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @MockitoSpyBean
    lateinit var jwtService: JwtService

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
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

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should allow authenticated users to call the mutation`() {
            val tokenValue = preconditions.validAccessToken.token
            val validTill = preconditions.validAccessToken.validTill

            doReturn("jwtTokenForSharedWorkspace").whenever(jwtService).buildJwtToken(argThat {
                userName == tokenValue
                        && isTransient
                        && roles.size == 1
                        && roles.contains(SaUserRoles.USER)
            }, eq(validTill))

            client
                .graphqlMutation { workspaceAccessTokenMutation(tokenValue) }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateAccessTokenByWorkspaceAccessToken to buildJsonObject {
                        put("accessToken", "jwtTokenForSharedWorkspace")
                        putJsonObject("workspace") {
                            put("id", preconditions.fryWorkspace.id)
                            put("name", preconditions.fryWorkspace.name)
                            put("defaultCurrency", preconditions.fryWorkspace.defaultCurrency)
                        }
                    }
                )
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = mustNotBeBlankTestCases("workspaceAccessToken", boundarySetup = ::setupBoundaryData) { value ->
            workspaceAccessTokenMutation(value)
        }

        private fun setupBoundaryData() {
            preconditions {
                workspaceAccessToken(
                    workspace = preconditions.fryWorkspace,
                    token = "a",
                    validTill = MOCK_TIME.plus(Duration.ofDays(42)),
                )
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .fromAnonymous()
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateAccessTokenByWorkspaceAccessToken)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return a JWT token for valid workspace access token`() {
            val tokenValue = preconditions.validAccessToken.token
            val validTill = preconditions.validAccessToken.validTill

            doReturn("jwtTokenForSharedWorkspace").whenever(jwtService).buildJwtToken(argThat {
                userName == tokenValue
                        && isTransient
                        && roles.size == 1
                        && roles.contains(SaUserRoles.USER)
            }, eq(validTill))

            client
                .graphqlMutation { workspaceAccessTokenMutation(tokenValue) }
                .fromAnonymous()
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateAccessTokenByWorkspaceAccessToken to buildJsonObject {
                        put("accessToken", "jwtTokenForSharedWorkspace")
                        putJsonObject("workspace") {
                            put("id", preconditions.fryWorkspace.id)
                            put("name", preconditions.fryWorkspace.name)
                            put("defaultCurrency", preconditions.fryWorkspace.defaultCurrency)
                        }
                    }
                )
        }

        @Test
        fun `should return INVALID_WORKSPACE_ACCESS_TOKEN error when token is not known`() {
            client
                .graphqlMutation { workspaceAccessTokenMutation("unknownToken") }
                .fromAnonymous()
                .executeAndVerifyBusinessError(
                    message = "Token unknownToken is not valid",
                    errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
                    path = DgsConstants.MUTATION.CreateAccessTokenByWorkspaceAccessToken
                )
        }

        @Test
        fun `should return INVALID_WORKSPACE_ACCESS_TOKEN error when token is revoked`() {
            client
                .graphqlMutation { workspaceAccessTokenMutation(preconditions.revokedAccessToken.token) }
                .fromAnonymous()
                .executeAndVerifyBusinessError(
                    message = "Token ${preconditions.revokedAccessToken.token} is not valid",
                    errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
                    path = DgsConstants.MUTATION.CreateAccessTokenByWorkspaceAccessToken
                )
        }

        @Test
        fun `should return INVALID_WORKSPACE_ACCESS_TOKEN error when token is expired`() {
            client
                .graphqlMutation { workspaceAccessTokenMutation(preconditions.expiredAccessToken.token) }
                .fromAnonymous()
                .executeAndVerifyBusinessError(
                    message = "Token ${preconditions.expiredAccessToken.token} is not valid",
                    errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
                    path = DgsConstants.MUTATION.CreateAccessTokenByWorkspaceAccessToken
                )
        }
    }

    private fun MutationProjection.workspaceAccessTokenMutation(
        workspaceAccessToken: String,
    ): MutationProjection = createAccessTokenByWorkspaceAccessToken(
        workspaceAccessToken = workspaceAccessToken
    ) {
        accessToken
        workspace {
            id
            name
            defaultCurrency
        }
    }
}
