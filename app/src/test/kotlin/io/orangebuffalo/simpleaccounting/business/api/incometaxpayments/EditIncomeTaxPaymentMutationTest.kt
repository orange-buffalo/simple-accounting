package io.orangebuffalo.simpleaccounting.business.api.incometaxpayments

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
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
import java.time.LocalDate

@DisplayName("editIncomeTaxPayment mutation")
class EditIncomeTaxPaymentMutationTest(
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
            val fryPayment = incomeTaxPayment(workspace = fryWorkspace)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    editIncomeTaxPaymentMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditIncomeTaxPayment)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    editIncomeTaxPaymentMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditIncomeTaxPayment)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    editIncomeTaxPaymentMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditIncomeTaxPayment)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                editIncomeTaxPaymentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryPayment.id!!,
                    title = value,
                )
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                editIncomeTaxPaymentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryPayment.id!!,
                    title = value,
                )
            },
            sizeConstraintTestCases("notes", maxLength = 1024) { value ->
                editIncomeTaxPaymentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryPayment.id!!,
                    notes = value,
                )
            },
            requiredFieldRejectedTestCases("id") {
                editIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryPayment.id!!)
            },
            requiredFieldRejectedTestCases("amount") {
                editIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryPayment.id!!)
            },
            requiredFieldRejectedTestCases("datePaid") {
                editIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryPayment.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                editIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryPayment.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditIncomeTaxPayment)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update all fields of an existing income tax payment`() {
            val payment = preconditions {
                incomeTaxPayment(
                    workspace = preconditions.fryWorkspace,
                    notes = "Old notes",
                )
            }

            client
                .graphqlMutation {
                    editIncomeTaxPaymentMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = payment.id!!,
                        title = "Updated Q2 Tax",
                        datePaid = LocalDate.of(3025, 7, 1),
                        reportingDate = LocalDate.of(3025, 6, 30),
                        amount = 75000,
                        notes = "Delivery to Omicron Persei 8 - updated",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditIncomeTaxPayment to buildJsonObject {
                        put("id", payment.id!!.toInt())
                        put("title", "Updated Q2 Tax")
                        put("datePaid", "3025-07-01")
                        put("reportingDate", "3025-06-30")
                        put("amount", 75000)
                        put("notes", "Delivery to Omicron Persei 8 - updated")
                    }
                )

            aggregateTemplate.findSingle<IncomeTaxPayment>(payment.id!!)
                .shouldBeEntityWithFields(
                    IncomeTaxPayment(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Updated Q2 Tax",
                        datePaid = LocalDate.of(3025, 7, 1),
                        reportingDate = LocalDate.of(3025, 6, 30),
                        amount = 75000,
                        notes = "Delivery to Omicron Persei 8 - updated",
                    )
                )
        }

        @Test
        fun `should clear optional notes`() {
            val payment = preconditions {
                incomeTaxPayment(
                    workspace = preconditions.fryWorkspace,
                    notes = "Old notes",
                )
            }

            client
                .graphqlMutation {
                    editIncomeTaxPaymentMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = payment.id!!,
                        title = "Q1 Tax",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditIncomeTaxPayment to buildJsonObject {
                        put("id", payment.id!!.toInt())
                        put("title", "Q1 Tax")
                        put("datePaid", MOCK_DATE.toString())
                        put("reportingDate", MOCK_DATE.toString())
                        put("amount", 100)
                        put("notes", JsonNull)
                    }
                )

            aggregateTemplate.findSingle<IncomeTaxPayment>(payment.id!!)
                .shouldBeEntityWithFields(
                    IncomeTaxPayment(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Q1 Tax",
                        datePaid = MOCK_DATE,
                        reportingDate = MOCK_DATE,
                        amount = 100,
                        notes = null,
                    )
                )
        }

        @Test
        fun `should return entity not found error for non-existent income tax payment`() {
            client
                .graphqlMutation {
                    editIncomeTaxPaymentMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = Long.MAX_VALUE,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditIncomeTaxPayment)
        }

        @Test
        fun `should return entity not found error for income tax payment in another user workspace`() {
            val zoidbergPayment = preconditions {
                incomeTaxPayment(workspace = preconditions.zoidbergWorkspace)
            }

            client
                .graphqlMutation {
                    editIncomeTaxPaymentMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        id = zoidbergPayment.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditIncomeTaxPayment)
        }
    }

    private fun MutationProjection.editIncomeTaxPaymentMutation(
        workspaceId: Long,
        id: Long,
        title: String = "Q1 Tax",
        datePaid: LocalDate = MOCK_DATE,
        reportingDate: LocalDate? = null,
        amount: Long = 100,
        notes: String? = null,
        attachments: List<Long>? = null,
    ): MutationProjection = editIncomeTaxPayment(
        workspaceId = workspaceId,
        id = id,
        title = title,
        datePaid = datePaid,
        reportingDate = reportingDate,
        amount = amount,
        notes = notes,
        attachments = attachments,
    ) {
        this.id
        this.title
        this.datePaid
        this.reportingDate
        this.amount
        this.notes
    }
}
