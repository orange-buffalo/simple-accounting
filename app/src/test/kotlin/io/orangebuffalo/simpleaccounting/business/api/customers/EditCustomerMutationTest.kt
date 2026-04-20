package io.orangebuffalo.simpleaccounting.business.api.customers

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.customers.Customer
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("editCustomer mutation")
class EditCustomerMutationTest(
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
            val fryCustomer = customer(workspace = fryWorkspace)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    editCustomerMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditCustomer)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    editCustomerMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditCustomer)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    editCustomerMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditCustomer)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("name") { value ->
                editCustomerMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryCustomer.id!!,
                    name = value,
                )
            },
            sizeConstraintTestCases("name", maxLength = 255) { value ->
                editCustomerMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryCustomer.id!!,
                    name = value,
                )
            },
            requiredFieldRejectedTestCases("id") {
                editCustomerMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryCustomer.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                editCustomerMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryCustomer.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditCustomer)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update the name of an existing customer`() {
            val customer = preconditions { customer(workspace = preconditions.fryWorkspace) }

            client
                .graphqlMutation {
                    editCustomerMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = customer.id!!,
                        name = "Slurm Inc",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditCustomer to buildJsonObject {
                        put("id", customer.id!!.toInt())
                        put("name", "Slurm Inc")
                    }
                )

            aggregateTemplate.findSingle<Customer>(customer.id!!)
                .shouldBeEntityWithFields(
                    Customer(
                        name = "Slurm Inc",
                        workspaceId = preconditions.fryWorkspace.id!!,
                    )
                )
        }

        @Test
        fun `should return entity not found error for non-existent customer`() {
            client
                .graphqlMutation {
                    editCustomerMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = Long.MAX_VALUE,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditCustomer)
        }

        @Test
        fun `should return entity not found error for customer in another user workspace`() {
            val zoidbergCustomer = preconditions { customer(workspace = preconditions.zoidbergWorkspace) }

            client
                .graphqlMutation {
                    editCustomerMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        id = zoidbergCustomer.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditCustomer)
        }
    }

    private fun MutationProjection.editCustomerMutation(
        workspaceId: Long,
        id: Long,
        name: String = "Planet Express",
    ): MutationProjection = editCustomer(
        workspaceId = workspaceId,
        id = id,
        name = name,
    ) {
        this.id
        this.name
    }
}
