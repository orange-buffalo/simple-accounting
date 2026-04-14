package io.orangebuffalo.simpleaccounting.business.api.invoices

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("cancelInvoice mutation")
class CancelInvoiceMutationTest(
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
                .graphqlMutation {
                    cancelInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        invoiceId = 1,
                    )
                }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CancelInvoice)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation {
                    cancelInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        invoiceId = 1,
                    )
                }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CancelInvoice)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation {
                    cancelInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        invoiceId = 1,
                    )
                }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CancelInvoice)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should cancel a draft invoice`() {
            val invoice = preconditions {
                invoice(customer = preconditions.fryCustomer)
            }

            client
                .graphqlMutation {
                    cancelInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        invoiceId = invoice.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CancelInvoice to buildJsonObject {
                        put("id", invoice.id!!.toInt())
                        put("status", "CANCELLED")
                    }
                )

            aggregateTemplate.findSingle<Invoice>(invoice.id!!).should {
                it.status.shouldBe(InvoiceStatus.CANCELLED)
                it.timeCancelled.shouldNotBe(null)
            }
        }

        @Test
        fun `should return entity not found error for non-existent invoice`() {
            client
                .graphqlMutation {
                    cancelInvoiceMutation(
                        workspaceId = preconditions.fryWorkspace.id!!,
                        invoiceId = Long.MAX_VALUE,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CancelInvoice)
        }

        @Test
        fun `should return entity not found error for invoice in another user workspace`() {
            val zoidbergCustomer = preconditions { customer(workspace = preconditions.zoidbergWorkspace) }
            val zoidbergInvoice = preconditions {
                invoice(customer = zoidbergCustomer)
            }

            client
                .graphqlMutation {
                    cancelInvoiceMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        invoiceId = zoidbergInvoice.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CancelInvoice)
        }
    }

    private fun MutationProjection.cancelInvoiceMutation(
        workspaceId: Long,
        invoiceId: Long,
    ): MutationProjection = cancelInvoice(
        workspaceId = workspaceId,
        invoiceId = invoiceId,
    ) {
        this.id
        this.status
    }
}
