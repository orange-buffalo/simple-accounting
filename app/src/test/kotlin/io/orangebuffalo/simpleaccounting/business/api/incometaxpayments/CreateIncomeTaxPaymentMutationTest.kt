package io.orangebuffalo.simpleaccounting.business.api.incometaxpayments

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.business.incometaxpayments.IncomeTaxPaymentAttachment
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
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
import java.time.LocalDate

@DisplayName("createIncomeTaxPayment mutation")
class CreateIncomeTaxPaymentMutationTest(
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
                .graphqlMutation { createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateIncomeTaxPayment)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateIncomeTaxPayment)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateIncomeTaxPayment)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            sizeConstraintTestCases("notes", maxLength = 1024) { value ->
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, notes = value)
            },
            optionalFieldAbsentTestCases("notes") {
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, notes = "x")
            },
            optionalFieldAbsentTestCases("reportingDate") {
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!, reportingDate = MOCK_DATE)
            },
            optionalFieldAbsentTestCases("attachments") {
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("amount") {
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("datePaid") {
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                createIncomeTaxPaymentMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateIncomeTaxPayment)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create an income tax payment with all fields`() {
            val document = preconditions { document(workspace = preconditions.fryWorkspace) }

            client
                .graphqlMutation {
                    createIncomeTaxPaymentMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Q1 Income Tax",
                        datePaid = LocalDate.of(3025, 1, 15),
                        reportingDate = LocalDate.of(3025, 1, 1),
                        amount = 50000,
                        notes = "Good news, everyone! Q1 tax payment complete",
                        attachments = listOf(document.id!!),
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateIncomeTaxPayment to buildJsonObject {
                        put("title", "Q1 Income Tax")
                        put("datePaid", "3025-01-15")
                        put("reportingDate", "3025-01-01")
                        put("amount", 50000)
                        put("notes", "Good news, everyone! Q1 tax payment complete")
                    }
                )

            aggregateTemplate.findAll<IncomeTaxPayment>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.title == "Q1 Income Tax" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    IncomeTaxPayment(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Q1 Income Tax",
                        datePaid = LocalDate.of(3025, 1, 15),
                        reportingDate = LocalDate.of(3025, 1, 1),
                        amount = 50000,
                        notes = "Good news, everyone! Q1 tax payment complete",
                        attachments = setOf(IncomeTaxPaymentAttachment(document.id!!)),
                    )
                )
        }

        @Test
        fun `should create an income tax payment without optional fields`() {
            client
                .graphqlMutation {
                    createIncomeTaxPaymentMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Annual Tax",
                        datePaid = LocalDate.of(3025, 4, 1),
                        amount = 100000,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateIncomeTaxPayment to buildJsonObject {
                        put("title", "Annual Tax")
                        put("datePaid", "3025-04-01")
                        put("reportingDate", "3025-04-01")
                        put("amount", 100000)
                        put("notes", JsonNull)
                    }
                )

            aggregateTemplate.findAll<IncomeTaxPayment>()
                .filter { it.workspaceId == preconditions.fryWorkspace.id && it.title == "Annual Tax" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    IncomeTaxPayment(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        title = "Annual Tax",
                        datePaid = LocalDate.of(3025, 4, 1),
                        reportingDate = LocalDate.of(3025, 4, 1),
                        amount = 100000,
                    )
                )
        }

        @Test
        fun `should return entity not found error for another user workspace`() {
            client
                .graphqlMutation {
                    createIncomeTaxPaymentMutation(workspaceId = preconditions.zoidbergWorkspace.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateIncomeTaxPayment)
        }
    }

    private fun MutationProjection.createIncomeTaxPaymentMutation(
        workspaceId: Long,
        title: String = "Q1 Tax",
        datePaid: LocalDate = MOCK_DATE,
        reportingDate: LocalDate? = null,
        amount: Long = 100,
        notes: String? = null,
        attachments: List<Long>? = null,
    ): MutationProjection = createIncomeTaxPayment(
        workspaceId = workspaceId,
        title = title,
        datePaid = datePaid,
        reportingDate = reportingDate,
        amount = amount,
        notes = notes,
        attachments = attachments,
    ) {
        this.title
        this.datePaid
        this.reportingDate
        this.amount
        this.notes
    }
}
