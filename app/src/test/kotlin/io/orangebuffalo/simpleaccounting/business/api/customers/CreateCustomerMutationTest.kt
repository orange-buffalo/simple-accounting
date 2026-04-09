package io.orangebuffalo.simpleaccounting.business.api.customers

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("createCustomer mutation")
class CreateCustomerMutationTest(
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
            val zoidberg = zoidberg()
            val zoidbergWorkspace = workspace(owner = zoidberg)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { createCustomerMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateCustomer)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createCustomerMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateCustomer)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createCustomerMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateCustomer)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("name") { value ->
                createCustomerMutation(workspaceId = preconditions.fryWorkspace.id!!, name = value)
            },
            sizeConstraintTestCases("name", maxLength = 255) { value ->
                createCustomerMutation(workspaceId = preconditions.fryWorkspace.id!!, name = value)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateCustomer)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create a new customer`() {
            client
                .graphqlMutation {
                    createCustomerMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        name = "MomCorp",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateCustomer to buildJsonObject {
                        put("name", "MomCorp")
                    }
                )

            aggregateTemplate.findAll<Customer>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.name == "MomCorp" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Customer(
                        name = "MomCorp",
                        workspaceId = preconditions.fryWorkspace.id!!,
                    )
                )
        }

        @Test
        fun `should return entity not found error for another user workspace`() {
            client
                .graphqlMutation {
                    createCustomerMutation(workspaceId = preconditions.zoidbergWorkspace.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateCustomer)
        }
    }

    private fun MutationProjection.createCustomerMutation(
        workspaceId: Long,
        name: String = "Planet Express",
    ): MutationProjection = createCustomer(
        workspaceId = workspaceId,
        name = name,
    ) {
        this.name
    }
}
