package io.orangebuffalo.simpleaccounting.business.api.generaltaxes

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
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

@DisplayName("createGeneralTax mutation")
class CreateGeneralTaxMutationTest(
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
                .graphqlMutation { createGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateGeneralTax)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateGeneralTax)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateGeneralTax)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                createGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                createGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            sizeConstraintTestCases("description", maxLength = 255) { value ->
                createGeneralTaxMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    description = value,
                )
            },
            numberRangeConstraintTestCases("rateInBps", minValue = 0, maxValue = 10000) { value ->
                createGeneralTaxMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    rateInBps = value,
                )
            },
            requiredFieldRejectedTestCases("rateInBps") {
                createGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                createGeneralTaxMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateGeneralTax)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create a new general tax with all fields`() {
            client
                .graphqlMutation {
                    createGeneralTaxMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "VAT",
                        description = "Value Added Tax for interplanetary deliveries",
                        rateInBps = 20_00,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateGeneralTax to buildJsonObject {
                        put("title", "VAT")
                        put("description", "Value Added Tax for interplanetary deliveries")
                        put("rateInBps", 20_00)
                    }
                )

            aggregateTemplate.findAll<GeneralTax>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.title == "VAT" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    GeneralTax(
                        title = "VAT",
                        workspaceId = preconditions.fryWorkspace.id!!,
                        description = "Value Added Tax for interplanetary deliveries",
                        rateInBps = 20_00,
                    )
                )
        }

        @Test
        fun `should create a general tax without optional description`() {
            client
                .graphqlMutation {
                    createGeneralTaxMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Sales Tax",
                        rateInBps = 10_00,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateGeneralTax to buildJsonObject {
                        put("title", "Sales Tax")
                        put("description", JsonNull)
                        put("rateInBps", 10_00)
                    }
                )
        }

        @Test
        fun `should return entity not found error for another user workspace`() {
            client
                .graphqlMutation {
                    createGeneralTaxMutation(workspaceId = preconditions.zoidbergWorkspace.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateGeneralTax)
        }
    }

    private fun MutationProjection.createGeneralTaxMutation(
        workspaceId: Long,
        title: String = "VAT",
        description: String? = null,
        rateInBps: Int = 10_00,
    ): MutationProjection = createGeneralTax(
        workspaceId = workspaceId,
        title = title,
        description = description,
        rateInBps = rateInBps,
    ) {
        this.title
        this.description
        this.rateInBps
    }
}
