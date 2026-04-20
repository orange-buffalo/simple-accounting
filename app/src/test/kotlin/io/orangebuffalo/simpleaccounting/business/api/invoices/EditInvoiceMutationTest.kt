package io.orangebuffalo.simpleaccounting.business.api.invoices

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
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

@DisplayName("editInvoice mutation")
class EditInvoiceMutationTest(
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
            val fryInvoice = invoice(customer = fryCustomer)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation {
                    editInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditInvoice)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    editInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditInvoice)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    editInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = 1,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditInvoice)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value ->
                editInvoiceMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryInvoice.id!!,
                    title = value,
                )
            },
            sizeConstraintTestCases("title", maxLength = 255) { value ->
                editInvoiceMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryInvoice.id!!,
                    title = value,
                )
            },
            mustNotBeBlankTestCases("currency") { value ->
                editInvoiceMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryInvoice.id!!,
                    currency = value,
                )
            },
            sizeConstraintTestCases("notes", maxLength = 1024) { value ->
                editInvoiceMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    id = preconditions.fryInvoice.id!!,
                    notes = value,
                )
            },
            requiredFieldRejectedTestCases("id") {
                editInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryInvoice.id!!)
            },
            requiredFieldRejectedTestCases("customerId") {
                editInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryInvoice.id!!)
            },
            requiredFieldRejectedTestCases("dateIssued") {
                editInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryInvoice.id!!)
            },
            requiredFieldRejectedTestCases("dueDate") {
                editInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryInvoice.id!!)
            },
            requiredFieldRejectedTestCases("amount") {
                editInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryInvoice.id!!)
            },
            requiredFieldRejectedTestCases("workspaceId") {
                editInvoiceMutation(workspaceId = preconditions.fryWorkspace.id!!, id = preconditions.fryInvoice.id!!)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditInvoice)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update all fields of an existing invoice`() {
            val invoice = preconditions {
                invoice(
                    customer = preconditions.fryCustomer,
                    notes = "Old delivery notes",
                )
            }
            val newCustomer = preconditions { customer(workspace = preconditions.fryWorkspace, name = "Planet Express") }

            client
                .graphqlMutation {
                    editInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = invoice.id!!,
                        customerId = newCustomer.id!!,
                        title = "Updated Slurm delivery",
                        dateIssued = LocalDate.of(3025, 3, 10),
                        dateSent = LocalDate.of(3025, 3, 11),
                        datePaid = LocalDate.of(3025, 4, 1),
                        dueDate = LocalDate.of(3025, 3, 31),
                        currency = "EUR",
                        amount = 3000,
                        notes = "Delivery to Omicron Persei 8 - updated",
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditInvoice to buildJsonObject {
                        put("id", invoice.id!!.toInt())
                        put("title", "Updated Slurm delivery")
                        put("dateIssued", "3025-03-10")
                        put("dateSent", "3025-03-11")
                        put("datePaid", "3025-04-01")
                        put("dueDate", "3025-03-31")
                        put("currency", "EUR")
                        put("amount", 3000)
                        put("notes", "Delivery to Omicron Persei 8 - updated")
                        put("status", "PAID")
                    }
                )

            aggregateTemplate.findSingle<Invoice>(invoice.id!!)
                .shouldBeEntityWithFields(
                    Invoice(
                        customerId = newCustomer.id!!,
                        title = "Updated Slurm delivery",
                        dateIssued = LocalDate.of(3025, 3, 10),
                        dateSent = LocalDate.of(3025, 3, 11),
                        datePaid = LocalDate.of(3025, 4, 1),
                        dueDate = LocalDate.of(3025, 3, 31),
                        currency = "EUR",
                        amount = 3000,
                        notes = "Delivery to Omicron Persei 8 - updated",
                        generalTaxId = null,
                        status = InvoiceStatus.PAID,
                    )
                )
        }

        @Test
        fun `should clear optional notes`() {
            val invoice = preconditions {
                invoice(
                    customer = preconditions.fryCustomer,
                    notes = "Moon cargo delivery",
                )
            }

            client
                .graphqlMutation {
                    editInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = invoice.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditInvoice to buildJsonObject {
                        put("id", invoice.id!!.toInt())
                        put("title", "Spaceship parts")
                        put("dateIssued", MOCK_DATE.toString())
                        put("dateSent", JsonNull)
                        put("datePaid", JsonNull)
                        put("dueDate", MOCK_DATE.toString())
                        put("currency", "USD")
                        put("amount", 100)
                        put("notes", JsonNull)
                        put("status", "DRAFT")
                    }
                )

            aggregateTemplate.findSingle<Invoice>(invoice.id!!)
                .shouldBeEntityWithFields(
                    Invoice(
                        customerId = preconditions.fryCustomer.id!!,
                        title = "Spaceship parts",
                        dateIssued = MOCK_DATE,
                        dateSent = null,
                        datePaid = null,
                        dueDate = MOCK_DATE,
                        currency = "USD",
                        amount = 100,
                        notes = null,
                        generalTaxId = null,
                        status = InvoiceStatus.DRAFT,
                    )
                )
        }

        @Test
        fun `should return entity not found error for non-existent invoice`() {
            client
                .graphqlMutation {
                    editInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        id = Long.MAX_VALUE,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditInvoice)
        }

        @Test
        fun `should return entity not found error for invoice in another user workspace`() {
            val zoidbergCustomer = preconditions { customer(workspace = preconditions.zoidbergWorkspace) }
            val zoidbergInvoice = preconditions {
                invoice(customer = zoidbergCustomer)
            }

            client
                .graphqlMutation {
                    editInvoiceMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        id = zoidbergInvoice.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditInvoice)
        }
    }

    private fun MutationProjection.editInvoiceMutation(
        workspaceId: Long,
        id: Long,
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
    ): MutationProjection = editInvoice(
        workspaceId = workspaceId,
        id = id,
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
        this.id
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
