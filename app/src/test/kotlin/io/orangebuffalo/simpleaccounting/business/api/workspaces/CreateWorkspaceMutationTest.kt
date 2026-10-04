package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("createWorkspace mutation")
class CreateWorkspaceMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val farnsworth = farnsworth()
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { createWorkspaceMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspace)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createWorkspaceMutation() }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspace)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createWorkspaceMutation() }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspace)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            countryCodeTestCases { value ->
                createWorkspaceMutation(
                    residency = value,
                    defaultCurrency = when (value) {
                        "AU" -> "AUD"
                        "UA" -> "UAH"
                        else -> "USD"
                    },
                )
            },
            mustNotBeBlankTestCases("name") { value ->
                createWorkspaceMutation(name = value)
            },
            sizeConstraintTestCases("name", maxLength = 255) { value ->
                createWorkspaceMutation(name = value)
            },
            mustNotBeBlankTestCases("defaultCurrency") { value ->
                createWorkspaceMutation(defaultCurrency = value)
            }.filterNot { it is GraphqlMutationValidBoundaryTestCase },
            sizeConstraintTestCases("defaultCurrency", maxLength = 3) { value ->
                createWorkspaceMutation(defaultCurrency = value)
            }.filterNot { it is GraphqlMutationValidBoundaryTestCase },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateWorkspace)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create a new workspace`() {
            client
                .graphqlMutation {
                    createWorkspaceMutation(
                        name = "Robot Arms Apts",
                        defaultCurrency = "EUR",
                        residency = "DE",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateWorkspace to buildJsonObject {
                        put("id", JsonValues.ANY_STRING)
                        put("name", "Robot Arms Apts")
                        put("defaultCurrency", "EUR")
                        put("residency", "DE")
                    }
                )

            aggregateTemplate.findAll<Workspace>()
                .filter { it.ownerId == preconditions.fry.id && it.name == "Robot Arms Apts" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Workspace(
                        name = "Robot Arms Apts",
                        defaultCurrency = "EUR",
                        ownerId = preconditions.fry.id!!,
                        residency = "DE",
                    )
                )
        }

        @ParameterizedTest
        @CsvSource("USD,AU", "AUD,US", "XXX,US", "usd,US")
        fun `should reject incompatible currency and residency without creating a workspace`(currency: String, residency: String) {
            val fry = preconditions.fry
            val before = aggregateTemplate.findAll<Workspace>()
            client.graphqlMutation { createWorkspaceMutation(defaultCurrency = currency, residency = residency) }
                .from(fry)
                .executeAndVerifyBusinessErrorCode(
                    errorCode = "INCOMPATIBLE_CURRENCY",
                    path = DgsConstants.MUTATION.CreateWorkspace,
                )
            aggregateTemplate.findAll<Workspace>().shouldBe(before)
        }

        @ParameterizedTest
        @CsvSource("USD,US", "USD,PA", "EUR,PL", "INR,BT")
        fun `should accept supported country currencies including additional currencies`(currency: String, residency: String) {
            client.graphqlMutation { createWorkspaceMutation(defaultCurrency = currency, residency = residency) }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateWorkspace to buildJsonObject {
                        put("id", JsonValues.ANY_STRING)
                        put("name", "Planet Express")
                        put("defaultCurrency", currency)
                        put("residency", residency)
                    }
                )
        }
    }

    private fun MutationProjection.createWorkspaceMutation(
        name: String = "Planet Express",
        defaultCurrency: String = "USD",
        residency: String = "US",
    ): MutationProjection = createWorkspace(
        name = name,
        defaultCurrency = defaultCurrency,
        residency = residency,
    ) {
        this.id
        this.name
        this.defaultCurrency
        this.residency
    }
}
