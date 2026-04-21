package io.orangebuffalo.simpleaccounting.business.api.invoices

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceAttachment
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
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

@DisplayName("createInvoice mutation")
class CreateInvoiceMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val fryCustomer = customer(workspace = fryWorkspace, name = "MomCorp")
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
                .graphqlMutation { createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateInvoice)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateInvoice)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateInvoice)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, title = value)
            },
            mustNotBeBlankTestCases("currency") { value ->
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, currency = value)
            },
            sizeConstraintTestCases("notes", maxLength = 1024) { value ->
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, notes = value)
            },
            requiredFieldRejectedTestCases("customerId") {
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("dateIssued") {
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("dueDate") {
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("amount") {
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                createInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateInvoice)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create an invoice with all fields`() {
            val document = preconditions { document(workspace = preconditions.fryWorkspace) }
            val generalTax = preconditions { generalTax(workspace = preconditions.fryWorkspace, rateInBps = 1000) }

            client
                .graphqlMutation {
                    createInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        customerId = preconditions.fryCustomer.id!!,
                        title = "Slurm delivery invoice",
                        dateIssued = LocalDate.of(3025, 1, 15),
                        dateSent = LocalDate.of(3025, 1, 16),
                        datePaid = LocalDate.of(3025, 2, 1),
                        dueDate = LocalDate.of(3025, 1, 30),
                        currency = "EUR",
                        amount = 5000,
                        notes = "Good news, everyone! Slurm delivery complete",
                        attachments = listOf(document.id!!),
                        generalTaxId = generalTax.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateInvoice to buildJsonObject {
                        put("title", "Slurm delivery invoice")
                        put("dateIssued", "3025-01-15")
                        put("dateSent", "3025-01-16")
                        put("datePaid", "3025-02-01")
                        put("dueDate", "3025-01-30")
                        put("currency", "EUR")
                        put("amount", 5000)
                        put("notes", "Good news, everyone! Slurm delivery complete")
                        put("status", "PAID")
                    }
                )

            aggregateTemplate.findAll<Invoice>()
                .filter { it.customerId == preconditions.fryCustomer.id && it.title == "Slurm delivery invoice" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Invoice(
                        customerId = preconditions.fryCustomer.id!!,
                        title = "Slurm delivery invoice",
                        dateIssued = LocalDate.of(3025, 1, 15),
                        dateSent = LocalDate.of(3025, 1, 16),
                        datePaid = LocalDate.of(3025, 2, 1),
                        dueDate = LocalDate.of(3025, 1, 30),
                        currency = "EUR",
                        amount = 5000,
                        notes = "Good news, everyone! Slurm delivery complete",
                        attachments = setOf(InvoiceAttachment(document.id!!)),
                        generalTaxId = generalTax.id,
                        status = InvoiceStatus.PAID,
                    )
                )
        }

        @Test
        fun `should create an invoice without optional fields`() {
            client
                .graphqlMutation {
                    createInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        customerId = preconditions.fryCustomer.id!!,
                        title = "Robot maintenance",
                        dateIssued = LocalDate.of(3025, 2, 1),
                        dueDate = LocalDate.of(3025, 2, 28),
                        currency = "USD",
                        amount = 200,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateInvoice to buildJsonObject {
                        put("title", "Robot maintenance")
                        put("dateIssued", "3025-02-01")
                        put("dateSent", JsonNull)
                        put("datePaid", JsonNull)
                        put("dueDate", "3025-02-28")
                        put("currency", "USD")
                        put("amount", 200)
                        put("notes", JsonNull)
                        put("status", "DRAFT")
                    }
                )

            aggregateTemplate.findAll<Invoice>()
                .filter { it.customerId == preconditions.fryCustomer.id && it.title == "Robot maintenance" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    Invoice(
                        customerId = preconditions.fryCustomer.id!!,
                        title = "Robot maintenance",
                        dateIssued = LocalDate.of(3025, 2, 1),
                        dateSent = null,
                        datePaid = null,
                        dueDate = LocalDate.of(3025, 2, 28),
                        currency = "USD",
                        amount = 200,
                        notes = null,
                        generalTaxId = null,
                        status = InvoiceStatus.DRAFT,
                    )
                )
        }

        @Test
        fun `should return entity not found error for another user workspace`() {
            client
                .graphqlMutation {
                    createInvoiceMutation(workspaceId = preconditions.zoidbergWorkspace.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateInvoice)
        }
    }

    private fun MutationProjection.createInvoiceMutation(
        workspaceId: Long,
        customerId: Long = preconditions.fryCustomer.id!!,
        title: String = "Spaceship parts",
        dateIssued: LocalDate = MOCK_DATE,
        dateSent: LocalDate? = null,
        datePaid: LocalDate? = null,
        dueDate: LocalDate = MOCK_DATE,
        currency: String = "USD",
        amount: Long = 100,
        notes: String? = null,
        attachments: List<Long>? = null,
        generalTaxId: Long? = null,
    ): MutationProjection = createInvoice(
        workspaceId = workspaceId,
        customerId = customerId,
        title = title,
        dateIssued = dateIssued,
        dateSent = dateSent,
        datePaid = datePaid,
        dueDate = dueDate,
        currency = currency,
        amount = amount,
        notes = notes,
        attachments = attachments,
        generalTaxId = generalTaxId,
    ) {
        this.title
        this.dateIssued
        this.dateSent
        this.datePaid
        this.dueDate
        this.currency
        this.amount
        this.notes
        this.status
    }
}
