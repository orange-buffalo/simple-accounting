package io.orangebuffalo.simpleaccounting.business.api.generaltaxes

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("editGeneralTax mutation")
class EditGeneralTaxMutationTest(
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
            val fryTax = generalTax(workspace = fryWorkspace)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    editGeneralTaxMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditGeneralTax)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    editGeneralTaxMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditGeneralTax)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    editGeneralTaxMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditGeneralTax)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                editGeneralTaxMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryTax.id!!,
                    title = value,
                )
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                editGeneralTaxMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryTax.id!!,
                    title = value,
                )
            },
            sizeConstraintTestCases("description", maxLength = 255) { value ->
                editGeneralTaxMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryTax.id!!,
                    description = value,
                )
            },
            numberRangeConstraintTestCases("rateInBps", minValue = 0, maxValue = 10000) { value ->
                editGeneralTaxMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryTax.id!!,
                    rateInBps = value,
                )
            },
            requiredFieldRejectedTestCases("id") {
                editGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryTax.id!!)
            },
            requiredFieldRejectedTestCases("rateInBps") {
                editGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryTax.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                editGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryTax.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditGeneralTax)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update all fields of an existing general tax`() {
            val tax = preconditions { generalTax(workspace = preconditions.fryWorkspace) }

            client
                .graphqlMutation {
                    editGeneralTaxMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = tax.id!!,
                        title = "GST",
                        description = "Goods and Services Tax for Moon deliveries",
                        rateInBps = 15_00,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditGeneralTax to buildJsonObject {
                        put("id", tax.id!!.toInt())
                        put("title", "GST")
                        put("description", "Goods and Services Tax for Moon deliveries")
                        put("rateInBps", 15_00)
                    }
                )

            aggregateTemplate.findSingle<GeneralTax>(tax.id!!)
                .shouldBeEntityWithFields(
                    GeneralTax(
                        title = "GST",
                        workspaceId = preconditions.fryWorkspace.id!!,
                        description = "Goods and Services Tax for Moon deliveries",
                        rateInBps = 15_00,
                    )
                )
        }

        @Test
        fun `should clear optional description`() {
            val tax = preconditions {
                generalTax(
                    workspace = preconditions.fryWorkspace,
                    description = "Old description",
                )
            }

            client
                .graphqlMutation {
                    editGeneralTaxMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = tax.id!!,
                        title = "VAT",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditGeneralTax to buildJsonObject {
                        put("id", tax.id!!.toInt())
                        put("title", "VAT")
                        put("description", JsonNull)
                        put("rateInBps", tax.rateInBps)
                    }
                )
        }

        @Test
        fun `should return entity not found error for non-existent general tax`() {
            client
                .graphqlMutation {
                    editGeneralTaxMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = Long.MAX_VALUE,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditGeneralTax)
        }

        @Test
        fun `should return entity not found error for general tax in another user workspace`() {
            val zoidbergTax = preconditions { generalTax(workspace = preconditions.zoidbergWorkspace) }

            client
                .graphqlMutation {
                    editGeneralTaxMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        id = zoidbergTax.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditGeneralTax)
        }
    }

    private fun MutationProjection.editGeneralTaxMutation(
        workspaceId: Long,
        id: Long,
        title: String = "VAT",
        description: String? = null,
        rateInBps: Int = 10_00,
    ): MutationProjection = editGeneralTax(
        workspaceId = workspaceId,
        id = id,
        title = title,
        description = description,
        rateInBps = rateInBps,
    ) {
        this.id
        this.title
        this.description
        this.rateInBps
    }
}
