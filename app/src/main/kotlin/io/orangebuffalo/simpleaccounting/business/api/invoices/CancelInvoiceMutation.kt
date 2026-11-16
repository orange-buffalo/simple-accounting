package io.orangebuffalo.simpleaccounting.business.api.invoices

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.invoices.InvoicesService
import io.orangebuffalo.simpleaccounting.business.customers.CustomersService
import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CancelInvoiceMutation(
    private val invoicesService: InvoicesService,
    private val customersService: CustomersService,
    private val workspacesService: WorkspacesService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription("Cancels an existing invoice in the specified workspace.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    fun cancelInvoice(
        @GraphQLDescription("ID of the workspace the invoice belongs to.")
        workspaceId: String,
        @GraphQLDescription("ID of the invoice to cancel.")
        invoiceId: String,
    ): InvoiceGqlDto {
        val existingInvoice = invoicesService.getInvoiceById(invoiceId)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")
        val customer = customersService.findById(existingInvoice.customerId)
            ?: throw EntityNotFoundException("Customer ${existingInvoice.customerId} is not found")
        workspacesService.validateWorkspaceAccess(
            workspaceId = customer.workspaceId,
            accessMode = WorkspaceAccessMode.READ_WRITE,
        )
        val invoice = invoicesService.cancelInvoice(invoice = existingInvoice, workspaceId = workspaceId)
        return invoice.toInvoiceGqlDto(workspaceId)
    }
}
