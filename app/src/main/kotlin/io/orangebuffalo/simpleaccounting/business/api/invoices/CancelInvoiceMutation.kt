package io.orangebuffalo.simpleaccounting.business.api.invoices

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesService
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CancelInvoiceMutation(
    private val invoicesService: InvoicesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Cancels an existing invoice in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    suspend fun cancelInvoice(
        @GraphQLDescription("ID of the workspace the invoice belongs to.")
        workspaceId: Long,
        @GraphQLDescription("ID of the invoice to cancel.")
        invoiceId: Long,
    ): InvoiceGqlDto {
        val invoice = invoicesService.cancelInvoice(invoiceId, workspaceId)
        return invoice.toInvoiceGqlDto(workspaceId)
    }
}
